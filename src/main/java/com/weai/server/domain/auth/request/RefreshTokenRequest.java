package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token request")
public record RefreshTokenRequest(
	@Schema(description = "Opaque refresh token issued at login")
	@NotBlank(message = "refreshToken is required.")
	String refreshToken
) {
}
