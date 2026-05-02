package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.google.GoogleTokenResponse;
import com.weai.server.domain.auth.dto.google.GoogleUserResponse;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${oauth.google.client-id}")
	private String clientId;

	@Value("${oauth.google.client-secret}")
	private String clientSecret;

	@Value("${oauth.google.redirect-uri}")
	private String redirectUri;

	@Value("${oauth.google.token-uri}")
	private String tokenUri;

	@Value("${oauth.google.user-info-uri}")
	private String userInfoUri;

	private final RestTemplate restTemplate;

	public SocialAuthorizationUrlResponse createAuthorizationUrl() {
		String authorizationUrl = UriComponentsBuilder
			.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", "openid email profile")
			.queryParam("access_type", "offline")
			.build()
			.encode()
			.toUriString();

		return new SocialAuthorizationUrlResponse("google", authorizationUrl, null);
	}

	public String getAccessToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("redirect_uri", redirectUri);
		params.add("code", code);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
		ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
			tokenUri,
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
			userInfoUri,
			HttpMethod.GET,
			request,
			GoogleUserResponse.class
		);

		return response.getBody();
	}
}
