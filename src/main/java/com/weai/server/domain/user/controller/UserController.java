package com.weai.server.domain.user.controller;

import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "Authenticated user API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@Operation(summary = "Get current user profile", description = "Return the currently authenticated user.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Current user loaded successfully"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication is required"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access token does not have user access", content = @Content)
	})
	@SwaggerErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND})
	@GetMapping("/me")
	public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
		return ApiResponse.success(userService.findByEmail(authentication.getName()));
	}
}
