package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.config.OAuthProperties;
import com.weai.server.domain.auth.dto.google.GoogleTokenResponse;
import com.weai.server.domain.auth.dto.google.GoogleUserResponse;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

	private final OAuthProperties oauthProperties;
	private final RestTemplate restTemplate;

	public SocialAuthorizationUrlResponse createAuthorizationUrl() {
		OAuthProperties.Google google = oauthProperties.getGoogle();

		String authorizationUrl = UriComponentsBuilder
			.fromUriString(google.getAuthorizationUri())
			.queryParam("client_id", google.getClientId())
			.queryParam("redirect_uri", google.getRedirectUri())
			.queryParam("response_type", "code")
			.queryParam("scope", "openid email profile")
			.queryParam("access_type", "offline")
			.build()
			.encode()
			.toUriString();

		return new SocialAuthorizationUrlResponse("google", authorizationUrl, null);
	}

	public String getAccessToken(String code) {
		OAuthProperties.Google google = oauthProperties.getGoogle();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", google.getClientId());
		params.add("client_secret", google.getClientSecret());
		params.add("redirect_uri", google.getRedirectUri());
		params.add("code", code);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
		ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
			google.getTokenUri(),
			request,
			GoogleTokenResponse.class
		);

		GoogleTokenResponse tokenBody = response.getBody();
		if (tokenBody == null || tokenBody.getAccessToken() == null) {
			throw new RuntimeException("Failed to issue Google access token.");
		}

		return tokenBody.getAccessToken();
	}

	public GoogleUserResponse getUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
		ResponseEntity<GoogleUserResponse> response = restTemplate.exchange(
			oauthProperties.getGoogle().getUserInfoUri(),
			HttpMethod.GET,
			request,
			GoogleUserResponse.class
		);

		return response.getBody();
	}
}
