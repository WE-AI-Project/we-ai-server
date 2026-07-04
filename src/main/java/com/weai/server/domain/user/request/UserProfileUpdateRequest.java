package com.weai.server.domain.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Current user profile update request")
public record UserProfileUpdateRequest(
	@Schema(description = "Login username", example = "kimcoding")
	@Size(max = 50, message = "username must be 50 characters or fewer.")
	String username,

	@Schema(description = "Display name", example = "Kim Coding")
	@Size(max = 20, message = "name must be 20 characters or fewer.")
	String name
) {
}
