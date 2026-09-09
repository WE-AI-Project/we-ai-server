package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to verify a six-digit sign-up email verification code")
public record SignupVerificationCodeVerifyRequest(
	@Schema(description = "Email address being verified", example = "royalkim@example.com")
	@NotBlank(message = "email is required.")
	@Email(message = "email must be a valid address.")
	String email,

	@Schema(description = "Six-digit verification code", example = "123456")
	@NotBlank(message = "verificationCode is required.")
	@Pattern(regexp = "\\d{6}", message = "verificationCode must be exactly 6 digits.")
	String verificationCode
) {
}
