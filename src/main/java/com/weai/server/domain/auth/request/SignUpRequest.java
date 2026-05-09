package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignUpRequest(
		@Schema(description = "고유한 로그인 아이디", example = "minhyeok")
		@NotBlank(message = "username is required.")
		@Size(min = 4, max = 50, message = "username must be between 4 and 50 characters.")
		String username,

		@Schema(description = "표시 이름", example = "김민혁")
		@NotBlank(message = "name is required.")
		@Size(max = 20, message = "name must be 20 characters or fewer.")
		String name,

		@Schema(description = "이메일 주소", example = "royalkim@example.com")
		@NotBlank(message = "email is required.")
		@Email(message = "email must be a valid address.")
		String email,

		@Schema(description = "평문 비밀번호", example = "kmh0707!")
		@NotBlank(message = "password is required.")
		@Size(min = 8, max = 100, message = "password must be between 8 and 100 characters.")
		String password
) {
}
