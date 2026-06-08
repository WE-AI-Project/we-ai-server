package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.google.GoogleUserResponse;
import com.weai.server.domain.auth.dto.naver.NaverUserResponse;
import com.weai.server.domain.auth.request.EmailCodeLoginRequest;
import com.weai.server.domain.auth.request.EmailLoginCodeSendRequest;
import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.auth.response.VerificationCodeDispatchResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final VerificationCodeService verificationCodeService;
    private final KakaoOAuthService kakaoOAuthService;
    private final NaverOAuthService naverOAuthService;
    private final GoogleOAuthService googleOAuthService;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = ensureUserExists(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Password does not match.");
        }

        return tokenService.issueTokens(user);
    }

    @Transactional
    public VerificationCodeDispatchResponse sendEmailLoginCode(EmailLoginCodeSendRequest request) {
        ensureUserExists(request.email());
        return verificationCodeService.sendEmailLoginCode(request);
    }

    @Transactional
    public TokenResponse loginWithEmailCode(EmailCodeLoginRequest request) {
        User user = ensureUserExists(request.email());
        verificationCodeService.verifyEmailLoginCode(request.email(), request.verificationCode());
        return tokenService.issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        return tokenService.refresh(rawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.logout(rawRefreshToken);
    }

    @Transactional
    public TokenResponse kakaoLogin(String code) {
        return kakaoOAuthService.loginOrSignUp(code);
    }

    @Transactional
    public TokenResponse naverLogin(String code, String state) {
        String naverAccessToken = naverOAuthService.getAccessToken(code, state);
        NaverUserResponse naverUserInfo = naverOAuthService.getUserInfo(naverAccessToken);
        NaverUserResponse.Response profile = naverUserInfo.getResponse();

        User user = userRepository.findByEmail(profile.getEmail())
                .orElseGet(() -> saveNaverUser(profile));

        return tokenService.issueTokens(user);
    }

    @Transactional
    public TokenResponse googleLogin(String code) {
        String googleAccessToken = googleOAuthService.getAccessToken(code);
        GoogleUserResponse profile = googleOAuthService.getUserInfo(googleAccessToken);

        User user = userRepository.findByEmail(profile.getEmail())
                .orElseGet(() -> saveGoogleUser(profile));

        return tokenService.issueTokens(user);
    }

    private User saveNaverUser(NaverUserResponse.Response profile) {
        String realName = profile.getName() != null ? profile.getName() : "Naver User";
        String username = profile.getNickname() != null ? profile.getNickname() : realName;

        if ("Naver User".equals(username)) {
            username = "naver_" + UUID.randomUUID().toString().substring(0, 8);
        }

        User newUser = User.create(
                username,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                realName,
                profile.getEmail(),
                UserRole.USER);

        return userRepository.save(newUser);
    }

    private User saveGoogleUser(GoogleUserResponse profile) {
        String realName = profile.getName() != null ? profile.getName() : "Google User";
        String username = profile.getEmail().split("@")[0];

        User newUser = User.create(
                username,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                realName,
                profile.getEmail(),
                UserRole.USER);

        return userRepository.save(newUser);
    }

    private User ensureUserExists(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "User with email '%s' could not be found.".formatted(email)));
    }
}
