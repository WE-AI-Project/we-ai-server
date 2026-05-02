package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.kakao.KakaoTokenResponse;
import com.weai.server.domain.auth.dto.kakao.KakaoUserResponse;
import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

	private static final long REFRESH_TOKEN_EXPIRES_IN_SECONDS = 7 * 24 * 60 * 60;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;

	@Value("${oauth.kakao.client-id}")
	private String clientId;

	@Value("${oauth.kakao.redirect-uri}")
	private String redirectUri;

	private final RestClient restClient = RestClient.create();

	public SocialAuthorizationUrlResponse createAuthorizationUrl() {
		String authorizationUrl = UriComponentsBuilder
			.fromUriString("https://kauth.kakao.com/oauth/authorize")
			.queryParam("response_type", "code")
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("prompt", "login")
			.build()
			.encode()
			.toUriString();

		return new SocialAuthorizationUrlResponse("kakao", authorizationUrl, null);
	}

	@Transactional
	public TokenResponse loginOrSignUp(String code) {
		String kakaoAccessToken = getKakaoAccessToken(code);
		KakaoUserResponse kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);
		User user = registerOrLoginKakaoUser(kakaoUserInfo);
		return issueTokens(user);
	}

	private TokenResponse issueTokens(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);
		long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

		String refreshTokenString = UUID.randomUUID().toString();
		Instant refreshTokenExpiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRES_IN_SECONDS);

		refreshTokenRepository.findByUserId(user.getId())
			.ifPresentOrElse(
				existingToken -> existingToken.rotate(refreshTokenString, refreshTokenExpiresAt),
				() -> refreshTokenRepository.save(
					com.weai.server.domain.auth.domain.RefreshToken.issue(user, refreshTokenString, refreshTokenExpiresAt)
				)
			);

		return new TokenResponse(
			"Bearer",
			accessToken,
			accessTokenExpiresIn,
			refreshTokenString,
			REFRESH_TOKEN_EXPIRES_IN_SECONDS,
			user.getUsername(),
			user.getEmail(),
			user.getRole()
		);
	}

	private String getKakaoAccessToken(String code) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", clientId);
		params.add("redirect_uri", redirectUri);
		params.add("code", code);

		KakaoTokenResponse response = restClient.post()
			.uri("https://kauth.kakao.com/oauth/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(params)
			.retrieve()
			.body(KakaoTokenResponse.class);

		return response.accessToken();
	}

	private KakaoUserResponse getKakaoUserInfo(String accessToken) {
		return restClient.get()
			.uri("https://kapi.kakao.com/v2/user/me")
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(KakaoUserResponse.class);
	}

	private User registerOrLoginKakaoUser(KakaoUserResponse kakaoInfo) {
		if (kakaoInfo.kakaoAccount() == null || kakaoInfo.kakaoAccount().profile() == null) {
			throw new IllegalArgumentException("Failed to load Kakao account profile.");
		}

		String kakaoNickname = kakaoInfo.kakaoAccount().profile().nickname();
		String kakaoEmail = kakaoInfo.kakaoAccount().email();

		if (kakaoEmail == null || kakaoEmail.isBlank()) {
			kakaoEmail = kakaoInfo.id() + "@kakao.user";
		}

		String uniqueUsername = "kakao_" + kakaoInfo.id();
		String finalKakaoEmail = kakaoEmail;

		return userRepository.findByEmail(finalKakaoEmail)
			.orElseGet(() -> userRepository.save(User.create(
				uniqueUsername,
				passwordEncoder.encode(UUID.randomUUID().toString()),
				kakaoNickname,
				finalKakaoEmail,
				UserRole.USER
			)));
	}
}
