package com.weai.server.domain.auth.controller;

import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.request.LogoutRequest;
import com.weai.server.domain.auth.request.RefreshTokenRequest;
import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.auth.service.AuthService;
import com.weai.server.domain.user.response.UserResponse;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "JWT authentication API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "Sign up", description = "Register a new user account in the database.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User registered successfully"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content)
	})
	@SecurityRequirements
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.CONFLICT})
	@PostMapping("/signup")
	public ApiResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ApiResponse.success(authService.signUp(request));
	}

	@Operation(
		summary = "Login and issue token pair",
		description = "Authenticate with database-backed credentials and issue both access and refresh tokens."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token issued successfully"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "Authentication failed",
			content = @Content
		)
	})
	@SecurityRequirements
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED})
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@Operation(
		summary = "Refresh access token",
		description = "Rotate the stored refresh token and issue a fresh access token pair."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Token refreshed successfully"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "Refresh token is invalid or expired",
			content = @Content
		)
	})
	@SecurityRequirements
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED})
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request));
	}

	@Operation(
		summary = "Logout",
		description = "Revoke the currently stored refresh token for the user session."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Refresh token revoked successfully"
		)
	})
	@SecurityRequirements
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT})
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request);
		return ApiResponse.successMessage("Logged out successfully.");
	}
}
