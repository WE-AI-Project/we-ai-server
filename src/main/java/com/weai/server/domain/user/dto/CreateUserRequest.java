package com.weai.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 생성 요청 DTO")
public record CreateUserRequest(
	@Schema(description = "사용자 이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
	String name,

	@Schema(description = "사용자 이메일", example = "gildong@example.com")
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이어야 합니다.")
	String email
) {
}
