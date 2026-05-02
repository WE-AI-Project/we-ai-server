package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.naver.NaverTokenResponse;
import com.weai.server.domain.auth.dto.naver.NaverUserResponse;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import java.util.UUID;
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
public class NaverOAuthService {

	@Value("${oauth.naver.client-id}")
	private String clientId;

	@Value("${oauth.naver.client-secret}")
	private String clientSecret;

	@Value("${oauth.naver.redirect-uri}")
	private String redirectUri;

	@Value("${oauth.naver.token-uri}")
	private String tokenUri;

	@Value("${oauth.naver.user-info-uri}")
	private String userInfoUri;

	private final RestTemplate restTemplate;

	public SocialAuthorizationUrlResponse createAuthorizationUrl() {
		String state = UUID.randomUUID().toString();
		String authorizationUrl = UriComponentsBuilder
			.fromUriString("https://nid.naver.com/oauth2.0/authorize")
			.queryParam("response_type", "code")
			.queryParam("client_id", clientId)
			.queryParam("state", state)
			.queryParam("redirect_uri", redirectUri)
			.build()
			.encode()
			.toUriString();

		return new SocialAuthorizationUrlResponse("naver", authorizationUrl, state);
	}

	public String getAccessToken(String code, String state) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("code", code);
		params.add("state", state);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
		ResponseEntity<NaverTokenResponse> response = restTemplate.postForEntity(
			tokenUri,
			request,
			NaverTokenResponse.class
		);

		NaverTokenResponse tokenBody = response.getBody();
		if (tokenBody == null || tokenBody.getAccessToken() == null) {
			throw new RuntimeException("Failed to issue Naver access token.");
		}

		return tokenBody.getAccessToken();
	}

	public NaverUserResponse getUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
		ResponseEntity<NaverUserResponse> response = restTemplate.exchange(
			userInfoUri,
			HttpMethod.GET,
			request,
			NaverUserResponse.class
		);

		return response.getBody();
	}
}
