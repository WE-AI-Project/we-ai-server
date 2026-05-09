package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record RefreshTokenRequest(
	@Schema(description = "로그인 시 발급된 리프레시 토큰")
	@NotBlank(message = "refreshToken is required.")
	String refreshToken
) {
}
