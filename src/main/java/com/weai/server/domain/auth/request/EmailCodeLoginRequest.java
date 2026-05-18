package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to log in with email and a six-digit verification code")
public record EmailCodeLoginRequest(
	@Schema(description = "User email address", example = "royalkim@example.com")
	@NotBlank(message = "email is required.")
	@Email(message = "email must be a valid address.")
	String email,

	@Schema(description = "Six-digit verification code", example = "123456")
	@NotBlank(message = "verificationCode is required.")
	@Pattern(regexp = "\\d{6}", message = "verificationCode must be exactly 6 digits.")
	String verificationCode
) {
}
