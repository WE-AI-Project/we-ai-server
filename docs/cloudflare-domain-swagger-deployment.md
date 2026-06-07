# Cloudflare 도메인 배포와 Swagger 접근 가이드

이 문서는 Cloudflare에서 관리하는 도메인을 이 서버에 연결하고, 애플리케이션을 배포한 뒤, Swagger UI로 API를 테스트하는 방법을 설명합니다.

## 권장 토폴로지

- 공개 API 도메인: `api.example.com`
- 선택 가능한 staging API 도메인: `api-stag.example.com`
- nginx 앞단에 Cloudflare 프록시 배치
- nginx가 `/api/*`, `/swagger-ui.html`, `/swagger-ui/*`, `/v3/api-docs*`, `/actuator/*` 요청을 Spring Boot 앱으로 전달

nginx 프록시 라우팅은 이미 [docker/nginx/default.conf.template](C:/Users/0122k/IdeaProjects/we-ai-server/docker/nginx/default.conf.template)에 설정되어 있습니다.

## 이 저장소에서 중요한 동작

- `dev` 는 기본적으로 Swagger가 활성화됩니다.
- `stag` 도 기본적으로 Swagger가 활성화되지만 `SWAGGER_ENABLED` 로 덮어쓸 수 있습니다.
- `prod` 는 기본적으로 Swagger가 비활성화되며 `SWAGGER_ENABLED=true` 일 때만 활성화됩니다.

관련 파일:

- [src/main/resources/application.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application.yml)
- [src/main/resources/application-stag.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-stag.yml)
- [src/main/resources/application-prod.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-prod.yml)
- [compose.yaml](C:/Users/0122k/IdeaProjects/we-ai-server/compose.yaml)

## 1. Cloudflare에서 도메인 준비

아래 중 하나에 대해 DNS 레코드를 만듭니다:

- 운영용 `api.example.com`
- 스테이징용 `api-stag.example.com`

권장 사항:

- 루트 도메인 대신 백엔드 전용 서브도메인을 사용합니다.
- 우선 staging에서 Swagger를 열고, 운영에서는 꼭 필요할 때만 엽니다.

## 2. 원본 서버 연결 방식 선택

### 옵션 A. 권장: Cloudflare Tunnel

공개 inbound 포트를 직접 열지 않고 서버를 노출하려면 Cloudflare Tunnel을 사용합니다.

Tunnel의 공개 hostname 대상은 아래로 지정합니다:

- nginx가 호스트에 노출되어 있다면 `http://localhost:80`

이 저장소는 Docker Compose를 통해 기본적으로 nginx를 호스트 `80` 포트에 publish 하도록 되어 있습니다.

### 옵션 B. DNS 레코드를 서버 공인 IP로 직접 연결

Cloudflare DNS 레코드를 서버 IP로 직접 연결하면 아래 항목도 함께 처리해야 합니다:

- inbound 방화벽 규칙
- Cloudflare와 origin 사이의 HTTPS
- origin 측 SSL 모드와 인증서 설정

가장 단순한 구성을 원한다면 Tunnel을 사용하세요.

## 3. 환경 파일 준비

아래 예시 파일 중 하나를 사용합니다:

- `.env.stag.example`
- `.env.prod.example`

일반적인 스테이징 값:

```env
APP_PROFILE=stag
NGINX_SERVER_NAME=api-stag.example.com
APP_BASE_URL=https://api-stag.example.com
APP_API_DOMAIN=api-stag.example.com
APP_FRONTEND_BASE_URL=https://stag.example.com
APP_FRONTEND_DOMAIN=stag.example.com
SWAGGER_ENABLED=true
KAKAO_REDIRECT_URI=https://api-stag.example.com/api/v1/auth/kakao/callback
NAVER_REDIRECT_URI=https://api-stag.example.com/api/v1/auth/naver/callback
GOOGLE_REDIRECT_URI=https://api-stag.example.com/api/v1/auth/google/callback
```

일반적인 운영 값:

```env
APP_PROFILE=prod
NGINX_SERVER_NAME=api.example.com
APP_BASE_URL=https://api.example.com
APP_API_DOMAIN=api.example.com
APP_FRONTEND_BASE_URL=https://app.example.com
APP_FRONTEND_DOMAIN=app.example.com
SWAGGER_ENABLED=false
KAKAO_REDIRECT_URI=https://api.example.com/api/v1/auth/kakao/callback
NAVER_REDIRECT_URI=https://api.example.com/api/v1/auth/naver/callback
GOOGLE_REDIRECT_URI=https://api.example.com/api/v1/auth/google/callback
```

참고:

- 각 제공자 콘솔에도 OAuth redirect URI를 함께 업데이트해야 합니다.
- `APP_BASE_URL` 은 Swagger와 OpenAPI 메타데이터에 사용되는 공개 API 도메인과 일치해야 합니다.

## 4. Docker Compose로 배포

스테이징 배포:

```bash
docker compose --env-file .env.stag --profile stag up -d --build
```

운영 배포:

```bash
docker compose --env-file .env.prod --profile prod up -d --build
```

## 5. 배포 검증

먼저 health를 확인합니다:

- `https://api-stag.example.com/actuator/health`
- `https://api.example.com/actuator/health`

그다음 Swagger UI를 엽니다:

- `https://api-stag.example.com/swagger-ui.html`
- `https://api.example.com/swagger-ui.html`

OpenAPI JSON 확인 주소:

- `https://api-stag.example.com/v3/api-docs`
- `https://api.example.com/v3/api-docs`

## 6. 더 안전한 운영 사용 방식

권장 방식:

- 운영에서는 기본적으로 Swagger를 꺼 둡니다.
- 일상적인 API 테스트는 staging을 사용합니다.
- 운영에서 꼭 필요할 때만 일시적으로 `SWAGGER_ENABLED=true` 로 설정합니다.
- 외부에서 접근 가능해야 한다면 Swagger와 OpenAPI 엔드포인트를 Cloudflare Access로 보호합니다.

권장 Cloudflare Access 보호 대상:

- `/swagger-ui.html`
- `/swagger-ui/*`
- `/v3/api-docs*`

## 7. 문제 해결

`swagger-ui.html` 이 열리지 않으면 아래를 확인합니다:

- 활성 프로필에 대해 `SWAGGER_ENABLED=true` 인지 확인
- `app` 컨테이너가 `SWAGGER_ENABLED` 값을 실제로 받았는지 확인
- nginx가 실행 중이고 호스트 `80` 포트에 publish 되었는지 확인
- Cloudflare hostname이 올바른 origin을 가리키는지 확인
- 우선 `/actuator/health` 가 접근 가능한지 확인

도메인 변경 후 OAuth 로그인이 깨지면 아래를 확인합니다:

- `KAKAO_REDIRECT_URI` 확인
- `NAVER_REDIRECT_URI` 확인
- `GOOGLE_REDIRECT_URI` 확인
- 각 제공자 콘솔에도 같은 redirect URI가 등록되어 있는지 확인
