package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "OAuth authorization code login request")
public record SocialCodeLoginRequest(
	@Schema(description = "Authorization code returned by the OAuth provider", example = "4/0AdQt8qj...")
	@NotBlank(message = "code is required.")
	String code
) {
}
