package com.weai.server.domain.auth.controller;

import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.request.LogoutRequest;
import com.weai.server.domain.auth.request.RefreshTokenRequest;
import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.auth.service.AuthService;
import com.weai.server.domain.auth.service.GoogleOAuthService;
import com.weai.server.domain.auth.service.KakaoOAuthService;
import com.weai.server.domain.auth.service.NaverOAuthService;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication, sign-up, token lifecycle, and social-login entry APIs.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;
	private final KakaoOAuthService kakaoOAuthService;
	private final NaverOAuthService naverOAuthService;
	private final GoogleOAuthService googleOAuthService;

	@Operation(summary = "Sign up", description = "Create a regular user account with email and password.")
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.CONFLICT})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest request) {
		userService.registerUser(request);
		return ApiResponse.successMessage("User registration completed successfully.");
	}

	@Operation(summary = "Login", description = "Authenticate with email and password, then issue JWT tokens.")
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@Operation(summary = "Refresh token", description = "Issue a new access token and rotate the refresh token.")
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request.refreshToken()));
	}

	@Operation(summary = "Logout", description = "Revoke the refresh token used by the current session.")
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.successMessage("Logged out successfully.");
	}

	@Operation(
		summary = "Kakao authorization URL",
		description = "Return the browser URL that starts the Kakao login flow."
	)
	@GetMapping("/kakao/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getKakaoAuthorizationUrl() {
		return ApiResponse.success(kakaoOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "Naver authorization URL",
		description = "Return the browser URL and state used to start the Naver login flow."
	)
	@GetMapping("/naver/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getNaverAuthorizationUrl() {
		return ApiResponse.success(naverOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "Google authorization URL",
		description = "Return the browser URL that starts the Google login flow."
	)
	@GetMapping("/google/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getGoogleAuthorizationUrl() {
		return ApiResponse.success(googleOAuthService.createAuthorizationUrl());
	}

	@Hidden
	@GetMapping("/kakao/callback")
	public ApiResponse<TokenResponse> kakaoLogin(@RequestParam("code") String code) {
		return ApiResponse.success(kakaoOAuthService.loginOrSignUp(code));
	}

	@Hidden
	@GetMapping("/naver/callback")
	public ApiResponse<TokenResponse> naverCallback(
		@RequestParam("code") String code,
		@RequestParam("state") String state
	) {
		return ApiResponse.success(authService.naverLogin(code, state));
	}

	@Hidden
	@GetMapping("/google/callback")
	public ApiResponse<TokenResponse> googleCallback(@RequestParam("code") String code) {
		return ApiResponse.success(authService.googleLogin(code));
	}
}
