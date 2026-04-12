package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.google.GoogleTokenResponse;
import com.weai.server.domain.auth.dto.google.GoogleUserResponse;
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

    // 1. 인가 코드로 구글 액세스 토큰 발급
    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri); // 구글은 필수 파라미터입니다.
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                tokenUri,
                request,
                GoogleTokenResponse.class
        );

        GoogleTokenResponse tokenBody = response.getBody();
        if (tokenBody == null || tokenBody.getAccessToken() == null) {
            throw new RuntimeException("구글 토큰 발급 실패");
        }

        return tokenBody.getAccessToken();
    }

    // 2. 액세스 토큰으로 유저 정보 가져오기
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