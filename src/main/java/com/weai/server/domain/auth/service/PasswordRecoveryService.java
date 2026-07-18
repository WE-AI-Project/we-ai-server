package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.config.AuthVerificationProperties;
import com.weai.server.domain.auth.request.PasswordFindRequest;
import com.weai.server.domain.auth.response.PasswordFindResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

	private static final String TEMPORARY_PASSWORD_CHARACTERS =
		"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
	private static final int TEMPORARY_PASSWORD_LENGTH = 12;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthVerificationProperties authVerificationProperties;
	private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

	@Value("${spring.mail.host:}")
	private String mailHost;

	@Value("${spring.mail.username:}")
	private String mailUsername;

	@Transactional
	public PasswordFindResponse issueTemporaryPassword(PasswordFindRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with email '%s' could not be found.".formatted(request.email())
			));

		String temporaryPassword = generateTemporaryPassword();
		user.updatePassword(passwordEncoder.encode(temporaryPassword));

		deliverTemporaryPassword(user, temporaryPassword);

		return new PasswordFindResponse(
			user.getEmail(),
			authVerificationProperties.isMockEnabled() ? "SIMULATED" : "SENT",
			authVerificationProperties.isExposeCodeInResponse() ? temporaryPassword : null
		);
	}

	private void deliverTemporaryPassword(User user, String temporaryPassword) {
		if (authVerificationProperties.isMockEnabled()) {
			log.info(
				"Simulated temporary password delivery. email={}, temporaryPassword={}",
				user.getEmail(),
				temporaryPassword
			);
			return;
		}

		JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
		if (!StringUtils.hasText(mailHost) || javaMailSender == null) {
			throw new ApiException(
				ErrorCode.PASSWORD_FIND_FAILED,
				"SMTP is not configured. Set MAIL_HOST or enable mock delivery for local testing."
			);
		}

		SimpleMailMessage message = new SimpleMailMessage();
		if (StringUtils.hasText(authVerificationProperties.getEmailFrom())) {
			message.setFrom(authVerificationProperties.getEmailFrom());
		} else if (StringUtils.hasText(mailUsername)) {
			message.setFrom(mailUsername);
		}
		message.setTo(user.getEmail());
		message.setSubject("[WE AI] Temporary password");
		message.setText(buildEmailBody(user.getName(), temporaryPassword));

		try {
			javaMailSender.send(message);
		} catch (MailException exception) {
			throw new ApiException(ErrorCode.PASSWORD_FIND_FAILED, "Failed to send the temporary password by email.");
		}
	}

	private String buildEmailBody(String name, String temporaryPassword) {
		return """
			Hello %s,

			Your temporary WE AI password is:
			%s

			Please log in with this temporary password and change it immediately.
			If you did not request password recovery, please contact support.
			""".formatted(name, temporaryPassword);
	}

	private String generateTemporaryPassword() {
		StringBuilder builder = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
		for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
			int characterIndex = RANDOM.nextInt(TEMPORARY_PASSWORD_CHARACTERS.length());
			builder.append(TEMPORARY_PASSWORD_CHARACTERS.charAt(characterIndex));
		}
		return builder.toString();
	}
}
