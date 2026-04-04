package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.domain.RefreshToken;
import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.request.LogoutRequest;
import com.weai.server.domain.auth.request.RefreshTokenRequest;
import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.security.jwt.JwtProperties;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final int REFRESH_TOKEN_BYTES = 48;
	private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final UserService userService;
	private final RefreshTokenRepository refreshTokenRepository;

	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public UserResponse signUp(SignUpRequest request) {
		return userService.registerUser(request);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);

		User user = userService.getUserEntityByUsername(authentication.getName());
		return issueTokenPair(user);
	}

	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request) {
		RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashRefreshToken(request.refreshToken()))
			.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid or expired."));

		Instant now = Instant.now();
		if (storedToken.isExpired(now)) {
			refreshTokenRepository.delete(storedToken);
			throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid or expired.");
		}

		User user = storedToken.getUser();
		return issueTokenPair(user, storedToken);
	}

	@Transactional
	public void logout(LogoutRequest request) {
		refreshTokenRepository.findByTokenHash(hashRefreshToken(request.refreshToken()))
			.ifPresent(refreshTokenRepository::delete);
	}

	private TokenResponse issueTokenPair(User user) {
		RefreshToken storedToken = refreshTokenRepository.findByUserId(user.getId()).orElse(null);
		return issueTokenPair(user, storedToken);
	}

	private TokenResponse issueTokenPair(User user, RefreshToken storedToken) {
		Instant refreshExpiresAt = Instant.now().plus(jwtProperties.getRefreshTokenExpiration());
		String rawRefreshToken = generateUniqueRefreshTokenValue();
		String refreshTokenHash = hashRefreshToken(rawRefreshToken);

		if (storedToken == null) {
			storedToken = RefreshToken.issue(user, refreshTokenHash, refreshExpiresAt);
		} else {
			storedToken.rotate(refreshTokenHash, refreshExpiresAt);
		}

		refreshTokenRepository.save(storedToken);

		String accessToken = jwtTokenProvider.createAccessToken(user);
		return new TokenResponse(
			"Bearer",
			accessToken,
			jwtTokenProvider.getAccessTokenExpirationSeconds(),
			rawRefreshToken,
			jwtProperties.getRefreshTokenExpiration().getSeconds(),
			user.getUsername(),
			user.getEmail(),
			user.getRole()
		);
	}

	private String generateUniqueRefreshTokenValue() {
		for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
			String candidate = generateRandomTokenValue();
			if (!refreshTokenRepository.existsByTokenHash(hashRefreshToken(candidate))) {
				return candidate;
			}
		}

		throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not generate a unique refresh token.");
	}

	private String generateRandomTokenValue() {
		byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	private String hashRefreshToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedToken = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashedToken);
		} catch (NoSuchAlgorithmException exception) {
			throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not hash refresh token.");
		}
	}
}
