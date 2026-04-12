package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.naver.NaverTokenResponse;
import com.weai.server.domain.auth.dto.naver.NaverUserResponse;
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
public class NaverOAuthService {

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    @Value("${oauth.naver.token-uri}")
    private String tokenUri;

    @Value("${oauth.naver.user-info-uri}")
    private String userInfoUri;

    private final RestTemplate restTemplate; // Config에서 Bean으로 등록되어 있어야 합니다.

    // 1. 인가 코드로 네이버 액세스 토큰 발급 받기
    public String getAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state); // 카카오와 달리 state가 필수 파라미터입니다.

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<NaverTokenResponse> response = restTemplate.postForEntity(
                tokenUri,
                request,
                NaverTokenResponse.class
        );

        NaverTokenResponse tokenBody = response.getBody();


        if (tokenBody == null || tokenBody.getAccessToken() == null) {
            throw new RuntimeException("네이버 토큰 발급 실패: " + tokenBody.getErrorDescription());
        }

        return response.getBody().getAccessToken();
    }

    // 2. 액세스 토큰으로 유저 정보 가져오기
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