package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Logout request")
public record LogoutRequest(
	@Schema(description = "Refresh token to revoke")
	@NotBlank(message = "refreshToken is required.")
	String refreshToken
) {
}
