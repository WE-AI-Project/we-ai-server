package com.weai.server.domain.auth.response;

import com.weai.server.domain.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답")
public record TokenResponse(
	@Schema(description = "토큰 타입", example = "Bearer")
	String tokenType,

	@Schema(description = "JWT 액세스 토큰")
	String accessToken,

	@Schema(description = "액세스 토큰 만료까지 남은 초", example = "3600")
	long accessTokenExpiresInSeconds,

	@Schema(description = "리프레시 토큰")
	String refreshToken,

	@Schema(description = "리프레시 토큰 만료까지 남은 초", example = "604800")
	long refreshTokenExpiresInSeconds,

	@Schema(description = "인증된 사용자 아이디", example = "admin")
	String username,

	@Schema(description = "인증된 사용자 이메일", example = "admin@example.com")
	String email,

	@Schema(description = "인증된 사용자 권한", example = "ADMIN")
	UserRole role
) {
}
