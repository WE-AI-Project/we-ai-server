package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Sign up request")
public record SignUpRequest(
	@Schema(description = "Unique login username", example = "honggildong")
	@NotBlank(message = "username is required.")
	@Size(min = 4, max = 50, message = "username must be between 4 and 50 characters.")
	String username,

	@Schema(description = "Display name", example = "Hong Gildong")
	@NotBlank(message = "name is required.")
	@Size(max = 20, message = "name must be 20 characters or fewer.")
	String name,

	@Schema(description = "Email address", example = "gildong@example.com")
	@NotBlank(message = "email is required.")
	@Email(message = "email must be a valid address.")
	String email,

	@Schema(description = "Plain text password", example = "password1234!")
	@NotBlank(message = "password is required.")
	@Size(min = 8, max = 100, message = "password must be between 8 and 100 characters.")
	String password
) {
}
