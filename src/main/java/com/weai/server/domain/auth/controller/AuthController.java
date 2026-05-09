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

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 갱신, 로그아웃, 소셜 로그인 진입을 위한 API입니다.  ㅗ")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;
	private final KakaoOAuthService kakaoOAuthService;
	private final NaverOAuthService naverOAuthService;
	private final GoogleOAuthService googleOAuthService;

	@Operation(summary = "회원가입", description = "이메일과 비밀번호로 일반 사용자 계정을 생성합니다.")
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.CONFLICT})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest request) {
		userService.registerUser(request);
		return ApiResponse.successMessage("회원가입이 완료되었습니다.");
	}

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 인증한 뒤 JWT 토큰을 발급합니다.")
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새 액세스 토큰을 발급하고 리프레시 토큰을 교체합니다.")
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request.refreshToken()));
	}

	@Operation(summary = "로그아웃", description = "현재 세션에서 사용 중인 리프레시 토큰을 무효화합니다.")
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.successMessage("로그아웃이 완료되었습니다.");
	}

	@Operation(
		summary = "카카오 로그인 URL 조회",
		description = "브라우저에서 카카오 로그인 절차를 시작할 수 있는 URL을 반환합니다."
	)
	@GetMapping("/kakao/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getKakaoAuthorizationUrl() {
		return ApiResponse.success(kakaoOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "네이버 로그인 URL 조회",
		description = "브라우저에서 네이버 로그인 절차를 시작할 수 있는 URL과 state 값을 반환합니다."
	)
	@GetMapping("/naver/url")
	public ApiResponse<SocialAuthorizationUrlResponse> getNaverAuthorizationUrl() {
		return ApiResponse.success(naverOAuthService.createAuthorizationUrl());
	}

	@Operation(
		summary = "구글 로그인 URL 조회",
		description = "브라우저에서 구글 로그인 절차를 시작할 수 있는 URL을 반환합니다."
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
