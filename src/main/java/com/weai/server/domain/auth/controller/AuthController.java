package com.weai.server.domain.auth.controller;

import com.weai.server.domain.auth.request.EmailCodeLoginRequest;
import com.weai.server.domain.auth.request.EmailLoginCodeSendRequest;
import com.weai.server.domain.auth.request.LoginRequest;
import com.weai.server.domain.auth.request.LogoutRequest;
import com.weai.server.domain.auth.request.NaverSocialCodeLoginRequest;
import com.weai.server.domain.auth.request.PasswordFindRequest;
import com.weai.server.domain.auth.request.RefreshTokenRequest;
import com.weai.server.domain.auth.request.SignUpRequest;
import com.weai.server.domain.auth.request.SignupVerificationCodeSendRequest;
import com.weai.server.domain.auth.request.SignupVerificationCodeVerifyRequest;
import com.weai.server.domain.auth.request.SocialCodeLoginRequest;
import com.weai.server.domain.auth.response.PasswordFindResponse;
import com.weai.server.domain.auth.response.SocialAuthorizationUrlResponse;
import com.weai.server.domain.auth.response.TokenResponse;
import com.weai.server.domain.auth.response.VerificationCodeDispatchResponse;
import com.weai.server.domain.auth.service.AuthService;
import com.weai.server.domain.auth.service.GoogleOAuthService;
import com.weai.server.domain.auth.service.KakaoOAuthService;
import com.weai.server.domain.auth.service.NaverOAuthService;
import com.weai.server.domain.auth.service.PasswordRecoveryService;
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
	private final PasswordRecoveryService passwordRecoveryService;

	@Operation(
		summary = "Sign up",
		description = "Creates a local account with email and password. The email must have already been "
			+ "verified via /signup/verification-code and /signup/verify."
	)
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.CONFLICT,
		ErrorCode.DUPLICATE_EMAIL,
		ErrorCode.EMAIL_NOT_VERIFIED
	})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpRequest request) {
		authService.ensureEmailVerifiedForSignup(request.email());
		userService.registerUser(request);
		return ApiResponse.successMessage("Sign-up completed successfully.");
	}

	@Operation(
		summary = "Send sign-up email verification code",
		description = "Sends a six-digit code to the given email to prove ownership before creating an account."
	)
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.DUPLICATE_EMAIL,
		ErrorCode.VERIFICATION_DELIVERY_FAILED
	})
	@PostMapping("/signup/verification-code")
	public ApiResponse<VerificationCodeDispatchResponse> sendSignupVerificationCode(
		@Valid @RequestBody SignupVerificationCodeSendRequest request
	) {
		return ApiResponse.success(authService.sendSignupVerificationCode(request));
	}

	@Operation(
		summary = "Verify sign-up email verification code",
		description = "Verifies the six-digit code sent to the given email. Must succeed before /signup."
	)
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.INVALID_VERIFICATION_CODE,
		ErrorCode.EXPIRED_VERIFICATION_CODE
	})
	@PostMapping("/signup/verify")
	public ApiResponse<Void> verifySignupVerificationCode(@Valid @RequestBody SignupVerificationCodeVerifyRequest request) {
		authService.verifySignupVerificationCode(request);
		return ApiResponse.successMessage("Email verified successfully.");
	}

	@Operation(summary = "Password login", description = "Issues JWT tokens with email and password.")
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.UNAUTHORIZED})
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@Operation(
		summary = "비밀번호 찾기",
		description = "가입된 이메일로 임시 비밀번호를 발급합니다. 로컬 mock 모드에서는 응답에 개발 확인용 임시 비밀번호가 포함됩니다."
	)
	@SwaggerErrorResponses({
		ErrorCode.INVALID_INPUT,
		ErrorCode.RESOURCE_NOT_FOUND,
		ErrorCode.PASSWORD_FIND_FAILED
	})
	@PostMapping("/password/find")
	public ApiResponse<PasswordFindResponse> findPassword(@Valid @RequestBody PasswordFindRequest request) {
		return ApiResponse.success(
			"PASSWORD_FIND_SUCCESS",
			"임시 비밀번호가 발급되었습니다.",
			passwordRecoveryService.issueTemporaryPassword(request)
		);
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
			  <title>SynAIpse VS Code Login</title>
			  <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%221.6%200.4%20208.6%20208.6%22%20role%3D%22img%22%20aria-label%3D%22SynAIpse%22%3E%3Crect%20x%3D%221.6%22%20y%3D%220.4%22%20width%3D%22208.6%22%20height%3D%22208.6%22%20rx%3D%2245.9%22%20fill%3D%22%230a0e17%22%2F%3E%3Cg%20fill%3D%22none%22%20stroke%3D%22%235b8cff%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpath%20d%3D%22M117.9%2098.4%20Q130.3%2098.6%20142.7%2097.5%22%20stroke-width%3D%227.5%22%2F%3E%3Cpath%20d%3D%22M142.7%2097.5%20Q149.4%20102.0%20157.5%20102.9%22%20stroke-width%3D%225.4%22%2F%3E%3Cpath%20d%3D%22M157.5%20102.9%20Q161.8%2097.9%20167.0%2093.7%22%20stroke-width%3D%223.9%22%2F%3E%3Cpath%20d%3D%22M167.0%2093.7%20Q169.7%2090.5%20173.0%2087.8%22%20stroke-width%3D%222.8%22%2F%3E%3Cpath%20d%3D%22M157.5%20102.9%20Q160.4%20108.3%20164.4%20112.8%22%20stroke-width%3D%223.9%22%2F%3E%3Cpath%20d%3D%22M164.4%20112.8%20Q167.7%20114.8%20170.8%20117.2%22%20stroke-width%3D%222.8%22%2F%3E%3Cpath%20d%3D%22M89.0%20114.3%20Q81.9%20124.9%2074.2%20135.0%22%20stroke-width%3D%227.5%22%2F%3E%3Cpath%20d%3D%22M74.2%20135.0%20Q71.7%20142.8%2066.8%20149.5%22%20stroke-width%3D%225.4%22%2F%3E%3Cpath%20d%3D%22M66.8%20149.5%20Q66.9%20154.8%2067.6%20160.1%22%20stroke-width%3D%223.9%22%2F%3E%3Cpath%20d%3D%22M67.6%20160.1%20Q68.7%20164.2%2069.0%20168.5%22%20stroke-width%3D%222.8%22%2F%3E%3Cpath%20d%3D%22M66.8%20149.5%20Q62.3%20152.2%2058.1%20155.3%22%20stroke-width%3D%223.9%22%2F%3E%3Cpath%20d%3D%22M58.1%20155.3%20Q57.9%20158.6%2057.5%20161.8%22%20stroke-width%3D%222.8%22%2F%3E%3Cpath%20d%3D%22M58.1%20155.3%20Q54.9%20155.8%2051.7%20155.4%22%20stroke-width%3D%222.8%22%2F%3E%3Cpath%20d%3D%22M85.9%2088.8%20Q78.1%2079.2%2068.3%2071.4%22%20stroke-width%3D%227.5%22%2F%3E%3Cpath%20d%3D%22M68.3%2071.4%20Q63.6%2065.4%2057.6%2060.7%22%20stroke-width%3D%225.4%22%2F%3E%3Cpath%20d%3D%22M57.6%2060.7%20Q52.3%2060.2%2047.2%2058.6%22%20stroke-width%3D%223.9%22%2F%3E%3Cpath%20d%3D%22M47.2%2058.6%20Q43.0%2058.2%2038.8%2057.5%22%20stroke-width%3D%222.8%22%2F%3E%3Cpath%20d%3D%22M57.6%2060.7%20Q57.0%2055.0%2054.8%2049.7%22%20stroke-width%3D%223.9%22%2F%3E%3Cpath%20d%3D%22M54.8%2049.7%20Q53.2%2045.4%2052.3%2040.9%22%20stroke-width%3D%222.8%22%2F%3E%3C%2Fg%3E%3Cg%20fill%3D%22%235b8cff%22%3E%3Ccircle%20cx%3D%22173.0%22%20cy%3D%2287.8%22%20r%3D%223.9%22%2F%3E%3Ccircle%20cx%3D%22170.8%22%20cy%3D%22117.2%22%20r%3D%223.9%22%2F%3E%3Ccircle%20cx%3D%2269.0%22%20cy%3D%22168.5%22%20r%3D%223.9%22%2F%3E%3Ccircle%20cx%3D%2257.5%22%20cy%3D%22161.8%22%20r%3D%223.9%22%2F%3E%3Ccircle%20cx%3D%2251.7%22%20cy%3D%22155.4%22%20r%3D%223.9%22%2F%3E%3Ccircle%20cx%3D%2238.8%22%20cy%3D%2257.5%22%20r%3D%223.9%22%2F%3E%3Ccircle%20cx%3D%2252.3%22%20cy%3D%2240.9%22%20r%3D%223.9%22%2F%3E%3C%2Fg%3E%3Ccircle%20cx%3D%22100%22%20cy%3D%22100%22%20r%3D%2218%22%20fill%3D%22%231fe7b6%22%2F%3E%3Ccircle%20cx%3D%22100%22%20cy%3D%22100%22%20r%3D%2218%22%20fill%3D%22none%22%20stroke%3D%22%230a0e17%22%20stroke-width%3D%224%22%2F%3E%3C%2Fsvg%3E">
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
			    .brand { display: flex; align-items: center; gap: 12px; }
			    .brand .mark { flex-shrink: 0; }
			    .brand .ai { color: #1fe7b6; }
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
			    .divider { margin-top: 6px; font-size: 12px; color: #9ca3af; text-align: center; }
			  </style>
			</head>
			<body>
			  <main>
			    <div class="brand">
			      <svg class="mark" width="44" height="44" xmlns="http://www.w3.org/2000/svg" viewBox="26.9 25.7 158.0 158.0" role="img" aria-label="SynAIpse"><g fill="none" stroke="#5b8cff" stroke-linecap="round" stroke-linejoin="round"><path d="M117.9 98.4 Q130.3 98.6 142.7 97.5" stroke-width="7.5"/><path d="M142.7 97.5 Q149.4 102.0 157.5 102.9" stroke-width="5.4"/><path d="M157.5 102.9 Q161.8 97.9 167.0 93.7" stroke-width="3.9"/><path d="M167.0 93.7 Q169.7 90.5 173.0 87.8" stroke-width="2.8"/><path d="M157.5 102.9 Q160.4 108.3 164.4 112.8" stroke-width="3.9"/><path d="M164.4 112.8 Q167.7 114.8 170.8 117.2" stroke-width="2.8"/><path d="M89.0 114.3 Q81.9 124.9 74.2 135.0" stroke-width="7.5"/><path d="M74.2 135.0 Q71.7 142.8 66.8 149.5" stroke-width="5.4"/><path d="M66.8 149.5 Q66.9 154.8 67.6 160.1" stroke-width="3.9"/><path d="M67.6 160.1 Q68.7 164.2 69.0 168.5" stroke-width="2.8"/><path d="M66.8 149.5 Q62.3 152.2 58.1 155.3" stroke-width="3.9"/><path d="M58.1 155.3 Q57.9 158.6 57.5 161.8" stroke-width="2.8"/><path d="M58.1 155.3 Q54.9 155.8 51.7 155.4" stroke-width="2.8"/><path d="M85.9 88.8 Q78.1 79.2 68.3 71.4" stroke-width="7.5"/><path d="M68.3 71.4 Q63.6 65.4 57.6 60.7" stroke-width="5.4"/><path d="M57.6 60.7 Q52.3 60.2 47.2 58.6" stroke-width="3.9"/><path d="M47.2 58.6 Q43.0 58.2 38.8 57.5" stroke-width="2.8"/><path d="M57.6 60.7 Q57.0 55.0 54.8 49.7" stroke-width="3.9"/><path d="M54.8 49.7 Q53.2 45.4 52.3 40.9" stroke-width="2.8"/></g><g fill="#5b8cff"><circle cx="173.0" cy="87.8" r="3.9"/><circle cx="170.8" cy="117.2" r="3.9"/><circle cx="69.0" cy="168.5" r="3.9"/><circle cx="57.5" cy="161.8" r="3.9"/><circle cx="51.7" cy="155.4" r="3.9"/><circle cx="38.8" cy="57.5" r="3.9"/><circle cx="52.3" cy="40.9" r="3.9"/></g><circle cx="100" cy="100" r="18" fill="#1fe7b6"/><circle cx="100" cy="100" r="18" fill="none" stroke="#111827" stroke-width="4"/></svg>
			      <h1>Syn<span class="ai">AI</span>pse 로그인</h1>
			    </div>
			    <p>로그인을 완료하면 VS Code 확장으로 자동 이동합니다.</p>
			    <label>
			      이메일
			      <input id="email" type="email" autocomplete="email" placeholder="email@example.com">
			    </label>
			    <label>
			      비밀번호
			      <input id="password" type="password" autocomplete="current-password" placeholder="비밀번호">
			    </label>
			    <button id="passwordLogin">비밀번호로 로그인</button>
			    <p class="divider">또는 이메일 인증 코드로 로그인</p>
			    <button id="sendCode" class="secondary">인증 코드 받기</button>
			    <label>
			      인증 코드
			      <input id="code" type="text" inputmode="numeric" maxlength="6" placeholder="6자리 코드">
			    </label>
			    <button id="login" class="secondary">인증 코드로 로그인</button>
			    <div id="status" class="status"></div>
			  </main>
			  <script>
			    const callbackUri = '__CALLBACK_URI__';
			    const email = document.getElementById('email');
			    const code = document.getElementById('code');
			    const password = document.getElementById('password');
			    const passwordLogin = document.getElementById('passwordLogin');
			    const sendCode = document.getElementById('sendCode');
			    const login = document.getElementById('login');
			    const status = document.getElementById('status');

			    passwordLogin.addEventListener('click', async () => {
			      await run(passwordLogin, async () => {
			        const userEmail = email.value.trim();
			        if (!userEmail || !password.value) throw new Error('이메일과 비밀번호를 모두 입력해 주세요.');
			        const payload = await request('/api/v1/auth/login', { email: userEmail, password: password.value });
			        redirectToVsCode(payload, userEmail);
			      });
			    });

			    password.addEventListener('keydown', (event) => {
			      if (event.key === 'Enter') passwordLogin.click();
			    });

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
			        redirectToVsCode(payload, userEmail);
			      });
			    });

			    function redirectToVsCode(payload, userEmail) {
			      const token = payload.data || payload;
			      if (!token.accessToken) throw new Error('로그인 응답에 accessToken이 없습니다.');
			      const params = new URLSearchParams();
			      params.set('accessToken', token.accessToken);
			      if (token.refreshToken) params.set('refreshToken', token.refreshToken);
			      params.set('email', token.email || userEmail);
			      window.location.href = callbackUri + (callbackUri.includes('?') ? '&' : '?') + params.toString();
			      setStatus('VS Code로 이동합니다. 브라우저가 확인 창을 띄우면 열기를 눌러 주세요.');
			    }

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
