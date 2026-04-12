package com.weai.server.domain.auth.controller;

import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.auth.service.AuthService;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.weai.server.domain.auth.service.KakaoOAuthService;

@Tag(name = "Auth", description = "인증 관련 API (로그인, 회원가입)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService; // 기존 유저 서비스 재활용

	@Operation(summary = "회원가입", description = "새로운 계정을 생성합니다.")
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest request) {
		// 기존에 이미 잘 구현되어 있는 UserService의 메서드를 호출합니다.
		userService.registerUser(request);
		return ApiResponse.successMessage("회원가입이 성공적으로 완료되었습니다.");
	}

	@Operation(summary = "이메일 로그인", description = "로그인하여 JWT 토큰을 발급받습니다.")
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		TokenResponse tokenResponse = authService.login(request);
		return ApiResponse.success(tokenResponse);
	}

	private final KakaoOAuthService kakaoOAuthService; // 주입 추가

	@Operation(summary = "카카오 로그인/회원가입", description = "카카오 인가 코드를 받아 소셜 로그인을 진행합니다.")
	@GetMapping("/kakao/callback")
	public ApiResponse<TokenResponse> kakaoLogin(@RequestParam("code") String code) {
		TokenResponse tokenResponse = kakaoOAuthService.loginOrSignUp(code);
		return ApiResponse.success(tokenResponse);
	}

	// 네이버에서 리다이렉트되는 주소 (브라우저가 GET으로 호출함)
	@GetMapping("/naver/callback")
	public ApiResponse<TokenResponse> naverCallback(
			@RequestParam("code") String code,
			@RequestParam("state") String state
	) {
		// 서비스의 naverLogin 로직 호출
		TokenResponse tokenResponse = authService.naverLogin(code, state);
		return ApiResponse.success(tokenResponse);
	}

	@GetMapping("/google/callback")
	public ApiResponse<TokenResponse> googleCallback(@RequestParam("code") String code) {
		// 구글은 state 파라미터를 강제하지 않으므로 code만 받아도 됩니다.
		TokenResponse tokenResponse = authService.googleLogin(code);
		return ApiResponse.success(tokenResponse);
	}
}