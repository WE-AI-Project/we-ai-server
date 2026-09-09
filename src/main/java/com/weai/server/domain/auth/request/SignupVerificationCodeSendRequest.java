package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to send a six-digit sign-up email verification code")
public record SignupVerificationCodeSendRequest(
	@Schema(description = "Email address to verify before sign-up", example = "royalkim@example.com")
	@NotBlank(message = "email is required.")
	@Email(message = "email must be a valid address.")
	String email
) {
}
