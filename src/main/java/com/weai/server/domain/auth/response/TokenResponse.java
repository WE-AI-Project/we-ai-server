package com.weai.server.domain.auth.response;

import com.weai.server.domain.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token response")
public record TokenResponse(
	@Schema(description = "Token type", example = "Bearer")
	String tokenType,

	@Schema(description = "JWT access token")
	String accessToken,

	@Schema(description = "Access token lifetime in seconds", example = "3600")
	long accessTokenExpiresInSeconds,

	@Schema(description = "Opaque refresh token")
	String refreshToken,

	@Schema(description = "Refresh token lifetime in seconds", example = "604800")
	long refreshTokenExpiresInSeconds,

	@Schema(description = "Authenticated username", example = "admin")
	String username,

	@Schema(description = "Authenticated email", example = "admin@example.com")
	String email,

	@Schema(description = "Authenticated user role", example = "ADMIN")
	UserRole role
) {
}
