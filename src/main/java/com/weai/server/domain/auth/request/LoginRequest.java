package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
		@Schema(description = "이메일 주소", example = "royalkim@example.com")
		@NotBlank(message = "email is required.")
		@Email(message = "email must be a valid address.")
		String email,

		@Schema(description = "비밀번호", example = "kmh0707!")
		@NotBlank(message = "password is required.")
		String password
) {
}
