package com.weai.server.domain.user.response;

import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User response")
public record UserResponse(
	@Schema(description = "User id", example = "1")
	Long id,

	@Schema(description = "Login username", example = "honggildong")
	String username,

	@Schema(description = "Display name", example = "Hong Gildong")
	String name,

	@Schema(description = "Email address", example = "gildong@example.com")
	String email,

	@Schema(description = "Assigned role", example = "USER")
	UserRole role
) {

	public static UserResponse from(User user) {
		return new UserResponse(
			user.getId(),
			user.getUsername(),
			user.getName(),
			user.getEmail(),
			user.getRole()
		);
	}
}
