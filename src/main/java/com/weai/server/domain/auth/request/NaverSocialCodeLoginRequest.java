package com.weai.server.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Naver OAuth authorization code login request")
public record NaverSocialCodeLoginRequest(
	@Schema(description = "Authorization code returned by Naver", example = "1f2dd6f2...")
	@NotBlank(message = "code is required.")
	String code,

	@Schema(description = "State returned by Naver", example = "8ceba6c4-9540-49db-9dca-2723e4a5a59d")
	@NotBlank(message = "state is required.")
	String state
) {
}
