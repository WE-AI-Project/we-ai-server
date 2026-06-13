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
import org.springframework.http.MediaType;
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
	@GetMapping(value = "/vscode/login", produces = MediaType.TEXT_HTML_VALUE)
	public String vscodeLoginPage(@RequestParam("callbackUri") String callbackUri) {
		return """
			<!doctype html>
			<html lang="ko">
			<head>
			  <meta charset="utf-8">
			  <meta name="viewport" content="width=device-width, initial-scale=1">
			  <title>SYNAIPSE VS Code Login</title>
			  <style>
			    :root { color-scheme: dark; }
			    * { box-sizing: border-box; }
			    body {
			      min-height: 100vh;
			      margin: 0;
			      display: grid;
			      place-items: center;
			      background: #111827;
			      color: #f9fafb;
			      font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
			    }
			    main {
			      width: min(420px, calc(100vw - 32px));
			      display: flex;
			      flex-direction: column;
			      gap: 14px;
			    }
			    h1 { margin: 0; font-size: 22px; letter-spacing: 0; }
			    p { margin: 0; color: #cbd5e1; line-height: 1.5; }
			    label { display: grid; gap: 6px; color: #d1d5db; font-size: 13px; }
			    input {
			      width: 100%;
			      min-height: 42px;
			      border: 1px solid #374151;
			      border-radius: 6px;
			      padding: 9px 11px;
			      background: #030712;
			      color: #f9fafb;
			      font: inherit;
			    }
			    button {
			      min-height: 42px;
			      border: 0;
			      border-radius: 6px;
			      padding: 10px 12px;
			      background: #2563eb;
			      color: #ffffff;
			      font: inherit;
			      font-weight: 700;
			      cursor: pointer;
			    }
			    button.secondary { background: #374151; }
			    button:disabled { opacity: .6; cursor: default; }
			    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
			    .status { min-height: 20px; color: #93c5fd; font-size: 13px; line-height: 1.45; }
			    .status.error { color: #fca5a5; }
			  </style>
			</head>
			<body>
			  <main>
			    <h1>SYNAIPSE 로그인</h1>
			    <p>이메일 인증을 완료하면 VS Code 확장으로 자동 이동합니다.</p>
			    <label>
			      이메일
			      <input id="email" type="email" autocomplete="email" placeholder="email@example.com">
			    </label>
			    <button id="sendCode" class="secondary">인증 코드 받기</button>
			    <label>
			      인증 코드
			      <input id="code" type="text" inputmode="numeric" maxlength="6" placeholder="6자리 코드">
			    </label>
			    <button id="login">VS Code로 로그인</button>
			    <div id="status" class="status"></div>
			  </main>
			  <script>
			    const callbackUri = '__CALLBACK_URI__';
			    const email = document.getElementById('email');
			    const code = document.getElementById('code');
			    const sendCode = document.getElementById('sendCode');
			    const login = document.getElementById('login');
			    const status = document.getElementById('status');

			    sendCode.addEventListener('click', async () => {
			      await run(sendCode, async () => {
			        const userEmail = email.value.trim();
			        if (!userEmail) throw new Error('이메일을 입력해 주세요.');
			        await request('/api/v1/auth/email-login/code', {
			          email: userEmail,
			          deliveryChannel: 'EMAIL'
			        });
			        setStatus('인증 코드를 보냈습니다. 메일함을 확인해 주세요.');
			      });
			    });

			    login.addEventListener('click', async () => {
			      await run(login, async () => {
			        const userEmail = email.value.trim();
			        const verificationCode = code.value.trim();
			        if (!userEmail || !verificationCode) throw new Error('이메일과 인증 코드를 모두 입력해 주세요.');
			        const payload = await request('/api/v1/auth/email-login', { email: userEmail, verificationCode });
			        const token = payload.data || payload;
			        if (!token.accessToken) throw new Error('로그인 응답에 accessToken이 없습니다.');
			        const params = new URLSearchParams();
			        params.set('accessToken', token.accessToken);
			        if (token.refreshToken) params.set('refreshToken', token.refreshToken);
			        params.set('email', token.email || userEmail);
			        window.location.href = callbackUri + (callbackUri.includes('?') ? '&' : '?') + params.toString();
			      });
			    });

			    async function request(path, body) {
			      const response = await fetch(path, {
			        method: 'POST',
			        headers: { 'Content-Type': 'application/json' },
			        body: JSON.stringify(body)
			      });
			      const text = await response.text();
			      const payload = text ? JSON.parse(text) : {};
			      if (!response.ok || payload.success === false) {
			        throw new Error(payload.message || text || '요청에 실패했습니다.');
			      }
			      return payload;
			    }

			    async function run(button, action) {
			      button.disabled = true;
			      setStatus('');
			      try {
			        await action();
			      } catch (error) {
			        setStatus(error instanceof Error ? error.message : String(error), true);
			      } finally {
			        button.disabled = false;
			      }
			    }

			    function setStatus(message, isError = false) {
			      status.textContent = message || '';
			      status.className = 'status' + (isError ? ' error' : '');
			    }
			  </script>
			</body>
			</html>
			""".replace("__CALLBACK_URI__", escapeJavaScriptString(callbackUri));
	}

	private String escapeJavaScriptString(String value) {
		return value
			.replace("\\", "\\\\")
			.replace("'", "\\'")
			.replace("\r", "")
			.replace("\n", "");
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
