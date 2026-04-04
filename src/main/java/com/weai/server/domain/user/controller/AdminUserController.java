package com.weai.server.domain.user.controller;

import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.dto.PageResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User", description = "ADMIN-only user management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

	private final UserService userService;

	@Operation(summary = "Get users", description = "Return registered users with pagination. Requires ADMIN role.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users loaded successfully"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication is required"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role is required")
	})
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.FORBIDDEN})
	@GetMapping
	public ApiResponse<PageResponse<UserResponse>> getUsers(
		@ParameterObject
		@PageableDefault(size = 20, sort = "id")
		Pageable pageable
	) {
		return ApiResponse.success(userService.findAll(pageable));
	}

	@Operation(summary = "Get user by id", description = "Return a single registered user by id. Requires ADMIN role.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User loaded successfully"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication is required"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role is required"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User could not be found")
	})
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.UNAUTHORIZED,
		ErrorCode.FORBIDDEN,
		ErrorCode.RESOURCE_NOT_FOUND
	})
	@GetMapping("/{userId}")
	public ApiResponse<UserResponse> getUser(
		@Parameter(description = "User id to load", example = "1")
		@PathVariable @Positive(message = "userId must be greater than or equal to 1.") Long userId
	) {
		return ApiResponse.success(userService.findById(userId));
	}
}
