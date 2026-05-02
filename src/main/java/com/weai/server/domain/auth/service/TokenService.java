package com.weai.server.domain.auth.service;

import com.weai.server.domain.auth.domain.RefreshToken;
import com.weai.server.domain.auth.repository.RefreshTokenRepository;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.user.domain.User;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.security.jwt.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

	private static final long REFRESH_TOKEN_EXPIRES_IN_SECONDS = 7 * 24 * 60 * 60;

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public TokenResponse issueTokens(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);
		long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

		String rawRefreshToken = UUID.randomUUID().toString();
		String refreshTokenHash = hashRefreshToken(rawRefreshToken);
		Instant refreshTokenExpiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRES_IN_SECONDS);

		refreshTokenRepository.findByUserId(user.getId())
			.ifPresentOrElse(
				existingToken -> existingToken.rotate(refreshTokenHash, refreshTokenExpiresAt),
				() -> refreshTokenRepository.save(
					RefreshToken.issue(user, refreshTokenHash, refreshTokenExpiresAt)
				)
			);

		return new TokenResponse(
			"Bearer",
			accessToken,
			accessTokenExpiresIn,
			rawRefreshToken,
			REFRESH_TOKEN_EXPIRES_IN_SECONDS,
			user.getUsername(),
			user.getEmail(),
			user.getRole()
		);
	}

	@Transactional
	public TokenResponse refresh(String rawRefreshToken) {
		RefreshToken refreshToken = getValidRefreshToken(rawRefreshToken);
		return issueTokens(refreshToken.getUser());
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(hashRefreshToken(rawRefreshToken))
			.ifPresent(refreshTokenRepository::delete);
	}

	private RefreshToken getValidRefreshToken(String rawRefreshToken) {
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashRefreshToken(rawRefreshToken))
			.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid."));

		if (refreshToken.isExpired(Instant.now())) {
			refreshTokenRepository.delete(refreshToken);
			throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token has expired.");
		}

		return refreshToken;
	}

	private String hashRefreshToken(String rawRefreshToken) {
		try {
			byte[] tokenHash = MessageDigest.getInstance("SHA-256")
				.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(tokenHash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is required to hash refresh tokens.", exception);
		}
	}
}
