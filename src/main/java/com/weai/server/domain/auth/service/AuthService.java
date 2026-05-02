package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.dto.google.GoogleUserResponse;
import com.weai.server.domain.auth.dto.naver.NaverUserResponse;
import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final long REFRESH_TOKEN_EXPIRES_IN_SECONDS = 7 * 24 * 60 * 60;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final NaverOAuthService naverOAuthService;
	private final GoogleOAuthService googleOAuthService;

	@Transactional
	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new ApiException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"User with email '%s' could not be found.".formatted(request.email())
			));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ApiException(ErrorCode.UNAUTHORIZED, "Password does not match.");
		}

		return issueTokens(user);
	}

	@Transactional
	public TokenResponse naverLogin(String code, String state) {
		String naverAccessToken = naverOAuthService.getAccessToken(code, state);
		NaverUserResponse naverUserInfo = naverOAuthService.getUserInfo(naverAccessToken);
		NaverUserResponse.Response profile = naverUserInfo.getResponse();

		User user = userRepository.findByEmail(profile.getEmail())
			.orElseGet(() -> saveNaverUser(profile));

		return issueTokens(user);
	}

	@Transactional
	public TokenResponse googleLogin(String code) {
		String googleAccessToken = googleOAuthService.getAccessToken(code);
		GoogleUserResponse profile = googleOAuthService.getUserInfo(googleAccessToken);

		User user = userRepository.findByEmail(profile.getEmail())
			.orElseGet(() -> saveGoogleUser(profile));

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
			UserRole.USER
		);

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
			UserRole.USER
		);

		return userRepository.save(newUser);
	}
}
