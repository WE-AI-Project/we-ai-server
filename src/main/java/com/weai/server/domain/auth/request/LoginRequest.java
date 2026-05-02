package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(
		@Schema(description = "Email address", example = "royalkim@example.com")
		@NotBlank(message = "email is required.")
		@Email(message = "email must be a valid address.")
		String email,

		@Schema(description = "Password", example = "kmh0707!")
		@NotBlank(message = "password is required.")
		String password
) {
}
