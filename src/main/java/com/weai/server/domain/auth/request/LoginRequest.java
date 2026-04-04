package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "JWT login request")
public record LoginRequest(
	@Schema(description = "Login username", example = "admin")
	@NotBlank(message = "username is required.")
	String username,

	@Schema(description = "Login password", example = "admin1234!")
	@NotBlank(message = "password is required.")
	String password
) {
}
