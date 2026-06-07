# Auth Swagger 테스트 가이드

이 문서는 Swagger UI에서 새 소셜 로그인 API와 6자리 이메일 로그인 흐름을 테스트하는 방법을 설명합니다.

## 1. 필요한 설정

### 빠른 Swagger 테스트용 로컬 모의 모드

실제로 이메일이나 카카오톡 메시지를 보내지 않고 API 흐름만 테스트하고 싶을 때 사용합니다.

```env
AUTH_VERIFICATION_MOCK_ENABLED=true
AUTH_VERIFICATION_EXPOSE_CODE_IN_RESPONSE=true
```

이 모드에서는:

- `POST /api/v1/auth/email-login/code` 응답에 `debugCode` 가 포함됩니다
- 실제 이메일이나 카카오톡 메시지는 전송되지 않습니다

### 실제 이메일 발송

Gmail, 네이버 메일 같은 실제 메일함으로 인증 코드를 보내려면 SMTP를 설정해야 합니다.

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-sender@example.com
MAIL_PASSWORD=your-app-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
AUTH_VERIFICATION_EMAIL_FROM=your-sender@example.com
AUTH_VERIFICATION_MOCK_ENABLED=false
AUTH_VERIFICATION_EXPOSE_CODE_IN_RESPONSE=false
```

참고:

- 발신자는 Gmail 계정, 회사 메일, 또는 서버에서 사용할 수 있는 아무 SMTP 계정이어도 됩니다.
- 수신자는 `@gmail.com`, `@naver.com` 을 포함한 유효한 메일함이면 됩니다.

### 실제 카카오톡 발송

카카오톡으로 인증 코드를 보내려면:

- Kakao Developers 콘솔에서 Kakao Login을 활성화합니다.
- `KAKAO_REDIRECT_URI` 와 일치하는 redirect URI를 등록합니다.
- `talk_message` 동의 항목을 활성화합니다.
- 카카오 메시지 링크에 사용하는 프론트엔드 URL을, 필요하다면 웹 도메인으로 등록합니다.

공식 문서:

- [Kakao Login overview](https://developers.kakao.com/docs/latest/en/kakaologin/common)
- [Kakao Talk Message REST API](https://developers.kakao.com/docs/latest/en/kakaotalk-message/rest-api)

## 2. Swagger 접속 주소

아래 주소를 엽니다:

- `http://localhost:8080/swagger-ui.html`

## 3. 이메일 코드 로그인 테스트

### 모의 모드

1. `POST /api/v1/auth/signup` 을 호출해 테스트 사용자를 만듭니다.
2. `POST /api/v1/auth/email-login/code` 를 호출합니다.

예시 요청:

```json
{
  "email": "tester@example.com",
  "deliveryChannel": "EMAIL"
}
```

3. 응답에서 `data.debugCode` 를 복사합니다.
4. `POST /api/v1/auth/email-login` 을 호출합니다.

예시 요청:

```json
{
  "email": "tester@example.com",
  "verificationCode": "123456"
}
```

5. `accessToken` 과 `refreshToken` 이 반환되는지 확인합니다.

### 실제 이메일 발송

1. SMTP를 설정하고 mock 모드를 비활성화합니다.
2. `deliveryChannel` 을 `EMAIL` 로 두고 `POST /api/v1/auth/email-login/code` 를 호출합니다.
3. Gmail 또는 네이버 메일 수신함을 확인합니다.
4. 수신한 6자리 코드로 `POST /api/v1/auth/email-login` 을 호출합니다.

## 4. 카카오톡 발송 테스트

1. `GET /api/v1/auth/kakao/message-url` 을 호출합니다.
2. 브라우저에서 `data.authorizationUrl` 을 엽니다.
3. 카카오 동의를 완료한 뒤 redirect URL의 `code` 쿼리 파라미터를 복사합니다.
4. `POST /api/v1/auth/email-login/code` 를 호출합니다.

예시 요청:

```json
{
  "email": "tester@example.com",
  "deliveryChannel": "KAKAO_TALK",
  "kakaoAuthorizationCode": "copied-code-from-kakao"
}
```

5. 카카오톡 메시지를 확인합니다.
6. 받은 6자리 코드로 `POST /api/v1/auth/email-login` 을 호출합니다.

프론트엔드에 이미 `talk_message` 동의가 포함된 카카오 사용자 access token이 있다면 `kakaoAuthorizationCode` 대신 그 토큰을 직접 보낼 수 있습니다.

```json
{
  "email": "tester@example.com",
  "deliveryChannel": "KAKAO_TALK",
  "kakaoAccessToken": "kakao-user-access-token"
}
```

## 5. 소셜 로그인 테스트

### Kakao

1. `GET /api/v1/auth/kakao/url` 을 호출합니다.
2. 브라우저에서 `data.authorizationUrl` 을 엽니다.
3. redirect URL의 `code` 쿼리 파라미터를 복사합니다.
4. `POST /api/v1/auth/kakao/login` 을 호출합니다.

```json
{
  "code": "copied-code-from-kakao"
}
```

### Naver

1. `GET /api/v1/auth/naver/url` 을 호출합니다.
2. 브라우저에서 `data.authorizationUrl` 을 엽니다.
3. redirect URL에서 `code` 와 `state` 를 모두 복사합니다.
4. `POST /api/v1/auth/naver/login` 을 호출합니다.

```json
{
  "code": "copied-code-from-naver",
  "state": "copied-state-from-naver"
}
```

### Google

1. `GET /api/v1/auth/google/url` 을 호출합니다.
2. 브라우저에서 `data.authorizationUrl` 을 엽니다.
3. redirect URL의 `code` 쿼리 파라미터를 복사합니다.
4. `POST /api/v1/auth/google/login` 을 호출합니다.

```json
{
  "code": "copied-code-from-google"
}
```

## 6. 권장 테스트 순서

1. `POST /api/v1/auth/signup`
2. `POST /api/v1/auth/email-login/code`
3. `POST /api/v1/auth/email-login`
4. `GET /api/v1/auth/kakao/url`
5. `POST /api/v1/auth/kakao/login`
6. `GET /api/v1/auth/naver/url`
7. `POST /api/v1/auth/naver/login`
8. `GET /api/v1/auth/google/url`
9. `POST /api/v1/auth/google/login`
