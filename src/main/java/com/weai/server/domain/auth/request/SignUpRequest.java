package com.weai.server.domain.auth.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "User sign-up request")
public record SignUpRequest(
	@Schema(
		description = "Optional login username. When omitted, the server generates one from the email address.",
		example = "minhyeok",
		nullable = true
	)
	@Size(min = 4, max = 50, message = "username must be between 4 and 50 characters.")
	String username,

	@Schema(description = "Display name", example = "Kim Minhyeok")
	@NotBlank(message = "name is required.")
	@Size(max = 20, message = "name must be 20 characters or fewer.")
	String name,

	@Schema(description = "Email address", example = "royalkim@example.com")
	@NotBlank(message = "email is required.")
	@Email(message = "email must be a valid address.")
	String email,

	@Schema(description = "Password", example = "kmh0707!")
	@NotBlank(message = "password is required.")
	@Size(min = 8, max = 100, message = "password must be between 8 and 100 characters.")
	String password
) {
}
