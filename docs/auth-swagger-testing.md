# Auth Swagger Testing Guide

This document explains how to test the new social login APIs and the six-digit email login flow in Swagger UI.

## 1. Required configuration

### Local mock mode for quick Swagger testing

Use this when you want to test the API flow without actually sending email or Kakao Talk messages.

```env
AUTH_VERIFICATION_MOCK_ENABLED=true
AUTH_VERIFICATION_EXPOSE_CODE_IN_RESPONSE=true
```

In this mode:

- `POST /api/v1/auth/email-login/code` returns `debugCode`
- No real email or Kakao Talk message is sent

### Real email delivery

To send the verification code to any real email inbox such as Gmail or Naver Mail, configure SMTP.

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

Notes:

- The sender can be a Gmail account, company mail, or any SMTP account your server can use.
- The receiver can be any valid mailbox, including `@gmail.com` and `@naver.com`.

### Real Kakao Talk delivery

To send the verification code to Kakao Talk:

- Enable Kakao Login in the Kakao Developers console.
- Register the redirect URI that matches `KAKAO_REDIRECT_URI`.
- Enable the `talk_message` consent item.
- Register the frontend URL used in Kakao message links if Kakao requires your web domain to be registered.

Official references:

- [Kakao Login overview](https://developers.kakao.com/docs/latest/en/kakaologin/common)
- [Kakao Talk Message REST API](https://developers.kakao.com/docs/latest/en/kakaotalk-message/rest-api)

## 2. Swagger entry

Open:

- `http://localhost:8080/swagger-ui.html`

## 3. Email-code login test

### Mock mode

1. Call `POST /api/v1/auth/signup` and create a test user.
2. Call `POST /api/v1/auth/email-login/code`.

Example request:

```json
{
  "email": "tester@example.com",
  "deliveryChannel": "EMAIL"
}
```

3. Copy `data.debugCode` from the response.
4. Call `POST /api/v1/auth/email-login`.

Example request:

```json
{
  "email": "tester@example.com",
  "verificationCode": "123456"
}
```

5. Confirm that `accessToken` and `refreshToken` are returned.

### Real email delivery

1. Configure SMTP and disable mock mode.
2. Call `POST /api/v1/auth/email-login/code` with `deliveryChannel` set to `EMAIL`.
3. Check the recipient inbox in Gmail or Naver Mail.
4. Call `POST /api/v1/auth/email-login` with the received six-digit code.

## 4. Kakao Talk delivery test

1. Call `GET /api/v1/auth/kakao/message-url`.
2. Open `data.authorizationUrl` in a browser.
3. Complete Kakao consent and copy the `code` query parameter from the redirect URL.
4. Call `POST /api/v1/auth/email-login/code`.

Example request:

```json
{
  "email": "tester@example.com",
  "deliveryChannel": "KAKAO_TALK",
  "kakaoAuthorizationCode": "copied-code-from-kakao"
}
```

5. Check your Kakao Talk message.
6. Call `POST /api/v1/auth/email-login` with the six-digit code you received.

If your frontend already has a Kakao user access token with `talk_message` consent, you can send that token directly instead of `kakaoAuthorizationCode`.

```json
{
  "email": "tester@example.com",
  "deliveryChannel": "KAKAO_TALK",
  "kakaoAccessToken": "kakao-user-access-token"
}
```

## 5. Social login test

### Kakao

1. Call `GET /api/v1/auth/kakao/url`.
2. Open `data.authorizationUrl` in a browser.
3. Copy the `code` query parameter from the redirect URL.
4. Call `POST /api/v1/auth/kakao/login`.

```json
{
  "code": "copied-code-from-kakao"
}
```

### Naver

1. Call `GET /api/v1/auth/naver/url`.
2. Open `data.authorizationUrl` in a browser.
3. Copy both `code` and `state` from the redirect URL.
4. Call `POST /api/v1/auth/naver/login`.

```json
{
  "code": "copied-code-from-naver",
  "state": "copied-state-from-naver"
}
```

### Google

1. Call `GET /api/v1/auth/google/url`.
2. Open `data.authorizationUrl` in a browser.
3. Copy the `code` query parameter from the redirect URL.
4. Call `POST /api/v1/auth/google/login`.

```json
{
  "code": "copied-code-from-google"
}
```

## 6. Recommended test order

1. `POST /api/v1/auth/signup`
2. `POST /api/v1/auth/email-login/code`
3. `POST /api/v1/auth/email-login`
4. `GET /api/v1/auth/kakao/url`
5. `POST /api/v1/auth/kakao/login`
6. `GET /api/v1/auth/naver/url`
7. `POST /api/v1/auth/naver/login`
8. `GET /api/v1/auth/google/url`
9. `POST /api/v1/auth/google/login`
