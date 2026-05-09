package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그아웃 요청")
public record LogoutRequest(
	@Schema(description = "무효화할 리프레시 토큰")
	@NotBlank(message = "refreshToken is required.")
	String refreshToken
) {
}
