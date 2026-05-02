package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.config.OAuthProperties;
import com.weai.server.domain.auth.dto.naver.NaverTokenResponse;
import com.weai.server.domain.auth.dto.naver.NaverUserResponse;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import java.util.UUID;
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
public class NaverOAuthService {

	private final OAuthProperties oauthProperties;
	private final RestTemplate restTemplate;

	public SocialAuthorizationUrlResponse createAuthorizationUrl() {
		OAuthProperties.Naver naver = oauthProperties.getNaver();
		String state = UUID.randomUUID().toString();
		String authorizationUrl = UriComponentsBuilder
			.fromUriString(naver.getAuthorizationUri())
			.queryParam("response_type", "code")
			.queryParam("client_id", naver.getClientId())
			.queryParam("state", state)
			.queryParam("redirect_uri", naver.getRedirectUri())
			.build()
			.encode()
			.toUriString();

		return new SocialAuthorizationUrlResponse("naver", authorizationUrl, state);
	}

	public String getAccessToken(String code, String state) {
		OAuthProperties.Naver naver = oauthProperties.getNaver();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", naver.getClientId());
		params.add("client_secret", naver.getClientSecret());
		params.add("code", code);
		params.add("state", state);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
		ResponseEntity<NaverTokenResponse> response = restTemplate.postForEntity(
			naver.getTokenUri(),
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
			oauthProperties.getNaver().getUserInfoUri(),
			HttpMethod.GET,
			request,
			NaverUserResponse.class
		);

		return response.getBody();
	}
}
