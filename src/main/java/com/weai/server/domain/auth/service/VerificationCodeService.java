package com.weai.server.domain.auth.service;
import com.weai.server.domain.auth.config.AuthVerificationProperties;
import com.weai.server.domain.auth.domain.VerificationCode;
import com.weai.server.domain.auth.domain.VerificationCodePurpose;
import com.weai.server.domain.auth.domain.VerificationDeliveryChannel;
import com.weai.server.domain.auth.repository.VerificationCodeRepository;
import com.weai.server.domain.auth.request.EmailLoginCodeSendRequest;
import com.weai.server.domain.auth.response.VerificationCodeDispatchResponse;
import com.weai.server.global.config.AppWebProperties;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

	private static final VerificationCodePurpose EMAIL_LOGIN = VerificationCodePurpose.EMAIL_LOGIN;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final VerificationCodeRepository verificationCodeRepository;
	private final AuthVerificationProperties authVerificationProperties;
	private final KakaoOAuthService kakaoOAuthService;
	private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
	private final AppWebProperties appWebProperties;
	private final RestClient restClient = RestClient.create();

	@Value("${spring.mail.host:}")
	private String mailHost;

	@Value("${spring.mail.username:}")
	private String mailUsername;

	@Transactional
	public VerificationCodeDispatchResponse sendEmailLoginCode(EmailLoginCodeSendRequest request) {
		validateDeliveryRequest(request);

		String rawCode = generateVerificationCode();
		LocalDateTime expiresAt = LocalDateTime.now().plus(authVerificationProperties.getExpiration());

		VerificationCode verificationCode = verificationCodeRepository.save(VerificationCode.issue(
			request.email(),
			EMAIL_LOGIN,
			request.deliveryChannel(),
			resolveDeliveryTarget(request),
			hashVerificationCode(rawCode),
			expiresAt
		));

		deliverVerificationCode(request, rawCode, expiresAt);

		return new VerificationCodeDispatchResponse(
			verificationCode.getPurpose(),
			verificationCode.getDeliveryChannel(),
			verificationCode.getDeliveryTarget(),
			authVerificationProperties.isMockEnabled() ? "SIMULATED" : "SENT",
			expiresAt,
			authVerificationProperties.isExposeCodeInResponse() ? rawCode : null
		);
	}

	@Transactional
	public void verifyEmailLoginCode(String email, String rawCode) {
		VerificationCode verificationCode = verificationCodeRepository
			.findTopByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, EMAIL_LOGIN)
			.orElseThrow(() -> new ApiException(
				ErrorCode.INVALID_VERIFICATION_CODE,
				"No active verification code was found for '%s'.".formatted(email)
			));

		LocalDateTime now = LocalDateTime.now();
		if (verificationCode.isExpired(now)) {
			verificationCode.markUsed(now);
			throw new ApiException(ErrorCode.EXPIRED_VERIFICATION_CODE, "Verification code has expired.");
		}

		if (!verificationCode.matches(hashVerificationCode(rawCode))) {
			throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE, "Verification code does not match.");
		}

		verificationCode.markUsed(now);
	}

	private void validateDeliveryRequest(EmailLoginCodeSendRequest request) {
		if (request.deliveryChannel() != VerificationDeliveryChannel.KAKAO_TALK) {
			return;
		}

		if (!StringUtils.hasText(request.kakaoAuthorizationCode()) && !StringUtils.hasText(request.kakaoAccessToken())) {
			throw new ApiException(
				ErrorCode.INVALID_INPUT,
				"kakaoAuthorizationCode or kakaoAccessToken is required for KAKAO_TALK delivery."
			);
		}
	}

	private void deliverVerificationCode(
		EmailLoginCodeSendRequest request,
		String rawCode,
		LocalDateTime expiresAt
	) {
		if (authVerificationProperties.isMockEnabled()) {
			log.info(
				"Simulated verification code delivery. email={}, channel={}, code={}, expiresAt={}",
				request.email(),
				request.deliveryChannel(),
				rawCode,
				expiresAt
			);
			return;
		}

		if (request.deliveryChannel() == VerificationDeliveryChannel.EMAIL) {
			sendEmail(request.email(), rawCode, expiresAt);
			return;
		}

		sendKakaoTalkMessage(resolveKakaoAccessToken(request), rawCode, expiresAt);
	}

	private void sendEmail(String email, String rawCode, LocalDateTime expiresAt) {
		JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
		if (!StringUtils.hasText(mailHost) || javaMailSender == null) {
			throw new ApiException(
				ErrorCode.VERIFICATION_DELIVERY_FAILED,
				"SMTP is not configured. Set MAIL_HOST or enable mock delivery for local testing."
			);
		}

		SimpleMailMessage message = new SimpleMailMessage();
		if (StringUtils.hasText(authVerificationProperties.getEmailFrom())) {
			message.setFrom(authVerificationProperties.getEmailFrom());
		} else if (StringUtils.hasText(mailUsername)) {
			message.setFrom(mailUsername);
		}
		message.setTo(email);
		message.setSubject(authVerificationProperties.getEmailSubject());
		message.setText(buildEmailBody(rawCode, expiresAt));

		try {
			javaMailSender.send(message);
		} catch (MailException exception) {
			throw new ApiException(
				ErrorCode.VERIFICATION_DELIVERY_FAILED,
				"Failed to send the verification code by email."
			);
		}
	}

	private void sendKakaoTalkMessage(String accessToken, String rawCode, LocalDateTime expiresAt) {
		MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
		requestBody.add("template_object", buildKakaoTemplate(rawCode, expiresAt));

		try {
			Map<?, ?> response = restClient.post()
				.uri(authVerificationProperties.getKakaoTalk().getMessageUri())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(requestBody)
				.retrieve()
				.body(Map.class);

			Object resultCode = response == null ? null : response.get("result_code");
			if (!Objects.equals(resultCode, 0) && !Objects.equals(resultCode, 0L)) {
				throw new ApiException(
					ErrorCode.VERIFICATION_DELIVERY_FAILED,
					"Kakao Talk message delivery failed."
				);
			}
		} catch (RestClientException exception) {
			throw new ApiException(
				ErrorCode.VERIFICATION_DELIVERY_FAILED,
				"Failed to send the verification code by Kakao Talk."
			);
		}
	}

	private String resolveKakaoAccessToken(EmailLoginCodeSendRequest request) {
		if (StringUtils.hasText(request.kakaoAccessToken())) {
			return request.kakaoAccessToken();
		}
		return kakaoOAuthService.exchangeAccessToken(request.kakaoAuthorizationCode());
	}

	private String buildKakaoTemplate(String rawCode, LocalDateTime expiresAt) {
		String frontendBaseUrl = escapeJson(appWebProperties.getFrontendBaseUrl());
		String text = escapeJson(buildKakaoText(rawCode, expiresAt));
		return """
			{
			  "object_type":"text",
			  "text":"%s",
			  "link":{
			    "web_url":"%s",
			    "mobile_web_url":"%s"
			  },
			  "button_title":"Open WE AI"
			}
			""".formatted(text, frontendBaseUrl, frontendBaseUrl).replace(System.lineSeparator(), "");
	}

	private String buildEmailBody(String rawCode, LocalDateTime expiresAt) {
		return """
			WE AI verification code: %s

			This code expires at %s.
			If you did not request this code, you can ignore this message.
			""".formatted(rawCode, expiresAt);
	}

	private String buildKakaoText(String rawCode, LocalDateTime expiresAt) {
		return """
			WE AI verification code: %s
			Expires at: %s
			""".formatted(rawCode, expiresAt);
	}

	private String resolveDeliveryTarget(EmailLoginCodeSendRequest request) {
		if (request.deliveryChannel() == VerificationDeliveryChannel.EMAIL) {
			return request.email();
		}
		return "kakao-talk";
	}

	private String generateVerificationCode() {
		int codeLength = authVerificationProperties.getCodeLength();
		StringBuilder builder = new StringBuilder(codeLength);
		for (int index = 0; index < codeLength; index++) {
			builder.append(RANDOM.nextInt(10));
		}
		return builder.toString();
	}

	private String hashVerificationCode(String rawCode) {
		try {
			byte[] codeHash = MessageDigest.getInstance("SHA-256")
				.digest(rawCode.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(codeHash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is required to hash verification codes.", exception);
		}
	}

	private String escapeJson(String value) {
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\r", "")
			.replace("\n", "\\n");
	}
}
