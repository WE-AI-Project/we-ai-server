package com.weai.server.domain.auth.request;

import com.weai.server.domain.auth.domain.VerificationDeliveryChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to send a six-digit email login verification code")
public record EmailLoginCodeSendRequest(
	@Schema(description = "User email address", example = "royalkim@example.com")
	@NotBlank(message = "email is required.")
	@Email(message = "email must be a valid address.")
	String email,

	@Schema(description = "Delivery channel for the verification code", example = "EMAIL")
	@NotNull(message = "deliveryChannel is required.")
	VerificationDeliveryChannel deliveryChannel,

	@Schema(
		description = "Kakao OAuth authorization code with talk_message consent. Required for KAKAO_TALK when access token is omitted.",
		example = "0f8c2a9db8...",
		nullable = true
	)
	String kakaoAuthorizationCode,

	@Schema(
		description = "Kakao user access token with talk_message consent. Required for KAKAO_TALK when authorization code is omitted.",
		example = "RjG7pX_6f5...",
		nullable = true
	)
	String kakaoAccessToken
) {
}
