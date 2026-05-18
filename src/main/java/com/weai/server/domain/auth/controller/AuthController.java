package com.weai.server.domain.auth.controller;

import com.weai.server.domain.auth.request.EmailCodeLoginRequest;
import com.weai.server.domain.auth.request.EmailLoginCodeSendRequest;
import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.request.LogoutRequest;
import com.weai.server.domain.auth.request.NaverSocialCodeLoginRequest;
import com.weai.server.domain.auth.request.RefreshTokenRequest;
import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.auth.request.SocialCodeLoginRequest;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.auth.response.VerificationCodeDispatchResponse;
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

@Tag(name = "Auth", description = "Authentication, social login, and email-code login APIs.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;
	private final KakaoOAuthService kakaoOAuthService;
	private final NaverOAuthService naverOAuthService;
	private final GoogleOAuthService googleOAuthService;

	@Operation(summary = "Sign up", description = "Creates a local account with email and password.")
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.CONFLICT})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest request) {
		userService.registerUser(request);
		return ApiResponse.successMessage("Sign-up completed successfully.");
	}

	@Operation(summary = "Password login", description = "Issues JWT tokens with email and password.")
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.UNAUTHORIZED})
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@Operation(
		summary = "Send email login code",
		description = "Sends a six-digit verification code for email login by email or Kakao Talk."
	)
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.RESOURCE_NOT_FOUND,
		ErrorCode.VERIFICATION_DELIVERY_FAILED
	})
	@PostMapping("/email-login/code")
	public ApiResponse<VerificationCodeDispatchResponse> sendEmailLoginCode(
		@Valid @RequestBody EmailLoginCodeSendRequest request
	) {
		return ApiResponse.success(authService.sendEmailLoginCode(request));
	}

	@Operation(
		summary = "Email-code login",
		description = "Logs in with email and a six-digit verification code, then issues JWT tokens."
	)
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.RESOURCE_NOT_FOUND,
		ErrorCode.INVALID_VERIFICATION_CODE,
		ErrorCode.EXPIRED_VERIFICATION_CODE
	})
	@PostMapping("/email-login")
	public ApiResponse<TokenResponse> loginWithEmailCode(@Valid @RequestBody EmailCodeLoginRequest request) {
		return ApiResponse.success(authService.loginWithEmailCode(request));
	}

	@Operation(summary = "Refresh token", description = "Reissues access and refresh tokens.")
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request.refreshToken()));
	}

	@Operation(summary = "Logout", description = "Invalidates the current refresh token.")
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.successMessage("Logout completed successfully.");
	}

	@Operation(
		summary = "Kakao login URL",
		description = "Returns the Kakao authorization URL used for social login."
	)
	@GetMapping("/kakao/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getKakaoAuthorizationUrl() {
		return ApiResponse.success(kakaoOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "Kakao Talk message consent URL",
		description = "Returns the Kakao authorization URL used to obtain talk_message consent for Kakao Talk delivery."
	)
	@GetMapping("/kakao/message-url")
	public ApiResponse<SocialAuthorizationUrlResponse> getKakaoMessageAuthorizationUrl() {
		return ApiResponse.success(kakaoOAuthService.createMessageAuthorizationUrl());
	}

	@Operation(
		summary = "Naver login URL",
		description = "Returns the Naver authorization URL and state used for social login."
	)
	@GetMapping("/naver/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getNaverAuthorizationUrl() {
		return ApiResponse.success(naverOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "Google login URL",
		description = "Returns the Google authorization URL used for social login."
	)
	@GetMapping("/google/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getGoogleAuthorizationUrl() {
		return ApiResponse.success(googleOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "Kakao social login",
		description = "Exchanges the Kakao authorization code for an application access token and refresh token."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/kakao/login")
	public ApiResponse<TokenResponse> kakaoLogin(@Valid @RequestBody SocialCodeLoginRequest request) {
		return ApiResponse.success(authService.kakaoLogin(request.code()));
	}

	@Operation(
		summary = "Naver social login",
		description = "Exchanges the Naver authorization code and state for an application access token and refresh token."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/naver/login")
	public ApiResponse<TokenResponse> naverLogin(@Valid @RequestBody NaverSocialCodeLoginRequest request) {
		return ApiResponse.success(authService.naverLogin(request.code(), request.state()));
	}

	@Operation(
		summary = "Google social login",
		description = "Exchanges the Google authorization code for an application access token and refresh token."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/google/login")
	public ApiResponse<TokenResponse> googleLogin(@Valid @RequestBody SocialCodeLoginRequest request) {
		return ApiResponse.success(authService.googleLogin(request.code()));
	}

	@Hidden
	@GetMapping("/kakao/callback")
	public ApiResponse<TokenResponse> kakaoCallback(@RequestParam("code") String code) {
		return ApiResponse.success(authService.kakaoLogin(code));
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
