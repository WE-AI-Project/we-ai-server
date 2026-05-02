package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.config.OAuthProperties;
import com.weai.server.domain.auth.dto.kakao.KakaoTokenResponse;
import com.weai.server.domain.auth.dto.kakao.KakaoUserResponse;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final OAuthProperties oauthProperties;
	private final RestClient restClient = RestClient.create();

	public SocialAuthorizationUrlResponse createAuthorizationUrl() {
		OAuthProperties.Kakao kakao = oauthProperties.getKakao();

		String authorizationUrl = UriComponentsBuilder
			.fromUriString(kakao.getAuthorizationUri())
			.queryParam("response_type", "code")
			.queryParam("client_id", kakao.getClientId())
			.queryParam("redirect_uri", kakao.getRedirectUri())
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
		return tokenService.issueTokens(user);
	}

	private String getKakaoAccessToken(String code) {
		OAuthProperties.Kakao kakao = oauthProperties.getKakao();
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", kakao.getClientId());
		params.add("redirect_uri", kakao.getRedirectUri());
		params.add("code", code);

		KakaoTokenResponse response = restClient.post()
			.uri(kakao.getTokenUri())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(params)
			.retrieve()
			.body(KakaoTokenResponse.class);

		return response.accessToken();
	}

	private KakaoUserResponse getKakaoUserInfo(String accessToken) {
		return restClient.get()
			.uri(oauthProperties.getKakao().getUserInfoUri())
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
