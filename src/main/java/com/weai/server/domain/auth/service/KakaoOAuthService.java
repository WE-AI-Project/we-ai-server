package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.domain.RefreshToken;
import com.weai.server.domain.auth.dto.kakao.KakaoTokenResponse;
import com.weai.server.domain.auth.dto.kakao.KakaoUserResponse;
import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    private final RestClient restClient = RestClient.create();

    @Transactional
    public TokenResponse loginOrSignUp(String code) {
        // 1. 카카오 Access Token 받아오기
        String kakaoAccessToken = getKakaoAccessToken(code);

        // 2. 카카오 사용자 정보 받아오기
        KakaoUserResponse kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        // 3. 우리 서비스 유저로 변환 (없으면 회원가입, 있으면 조회)
        User user = registerOrLoginKakaoUser(kakaoUserInfo);

        // 4. JWT 토큰 발급 및 응답 (기존 로그인 로직과 동일)
        String accessToken = jwtTokenProvider.createAccessToken(user);
        long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();
        String refreshTokenString = UUID.randomUUID().toString();
        long refreshTokenExpiresIn = 604800;

        RefreshToken refreshToken = RefreshToken.issue(user, refreshTokenString, Instant.now().plusSeconds(refreshTokenExpiresIn));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                "Bearer", accessToken, accessTokenExpiresIn,
                refreshTokenString, refreshTokenExpiresIn,
                user.getUsername(), user.getEmail(), user.getRole()
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
        // 1. 카카오 계정 정보가 아예 없는 경우 예외 처리
        if (kakaoInfo.kakaoAccount() == null || kakaoInfo.kakaoAccount().profile() == null) {
            throw new IllegalArgumentException("카카오 계정 정보를 가져올 수 없습니다.");
        }

        String kakaoNickname = kakaoInfo.kakaoAccount().profile().nickname();
        String kakaoEmail = kakaoInfo.kakaoAccount().email();

        // 2. 사용자가 이메일 제공에 동의하지 않은 경우 가짜 이메일 생성 (DB 충돌 방지)
        if (kakaoEmail == null || kakaoEmail.isBlank()) {
            kakaoEmail = kakaoInfo.id() + "@kakao.user";
        }

        String uniqueUsername = "kakao_" + kakaoInfo.id();
        String finalKakaoEmail = kakaoEmail; // 람다식 내부에서 쓰기 위한 final 처리

        // 3. 이메일로 기존 가입 여부 확인 및 자동 회원가입
        return userRepository.findByEmail(finalKakaoEmail)
                .orElseGet(() -> {
                    User newUser = User.create(
                            uniqueUsername,
                            passwordEncoder.encode(UUID.randomUUID().toString()),
                            kakaoNickname,
                            finalKakaoEmail,
                            UserRole.USER
                    );
                    return userRepository.save(newUser);
                });
    }
}