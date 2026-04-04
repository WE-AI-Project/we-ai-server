# we-ai-server

Spring Boot 기반 백엔드 보일러플레이트 프로젝트입니다.  
공통 응답 포맷, 전역 예외 처리, 요청/응답 로깅, JWT 인증, refresh token 저장소, 권한 분리, Swagger 문서화, 로컬 MySQL 실행 환경까지 포함한 상태입니다.

## 백엔드 보일러플레이트 구성 내용

현재 이 프로젝트에는 아래 항목들이 기본 골격으로 포함되어 있습니다.

- 공통 API 응답 포맷
  - `ApiResponse<T>` 기반 성공/실패 응답 통일
- 전역 예외 처리
  - 검증 오류, 400/401/403/404/405/415, 서버 예외 공통 처리
- 요청/응답 로깅
  - `X-Request-Id` 생성
  - 요청/응답 헤더 및 본문 로깅
  - `Authorization`, `password`, `token`, `refreshToken` 등 민감값 마스킹
- JPA Auditing
  - `createdAt`, `updatedAt` 공통 필드 자동 관리
- 인증/인가
  - JWT access token 기반 인증
  - refresh token DB 저장 및 재발급
  - refresh token은 원문이 아니라 해시로 저장
- 권한 분리
  - 일반 사용자: `/api/v1/users/me`
  - 관리자: `/api/v1/admin/users/**`
- Swagger/OpenAPI
  - Swagger UI 및 bearer token 테스트 지원
- 로컬 개발 환경
  - Docker Compose 기반 nginx, MinIO, MySQL 실행
  - `.env` 기반 환경변수 관리

## 기술 스택

- Java 17
- Spring Boot 4.0.x
- Spring Web MVC
- Spring Security
- Spring Data JPA
- MySQL 8.4
- Docker Compose
- Swagger / OpenAPI
- Gradle Wrapper

## 실행 프로필 기준

이 프로젝트는 프로필별로 동작 목적이 다릅니다.

- 기본 프로필
  - 운영에 더 가까운 안전한 기본값
  - `ddl-auto: none`
  - Docker Compose 자동 실행 비활성화
  - 기본 관리자 계정 자동 생성 없음
- `dev` 프로필
  - 로컬 개발용 편의 설정
  - `ddl-auto: update`
  - Docker Compose 자동 연동 활성화
  - 개발용 부트스트랩 관리자 계정 생성 가능
- `test` 프로필
  - H2 기반 테스트 환경

즉, 로컬 개발 시에는 `dev` 프로필로 실행하는 것을 기준으로 사용합니다.

## 권장 개발 환경

- IntelliJ IDEA
- Docker Desktop
- Java 17
- MySQL Workbench
- PowerShell

## 환경변수

프로젝트 루트에 `.env` 파일을 두고 사용합니다.  
예시는 [`.env.example`](C:/Users/0122k/IdeaProjects/we-ai-server/.env.example)에 있습니다.

예시:

```env
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=we_ai
DB_USERNAME=weai
DB_PASSWORD=change-me
DB_ROOT_PASSWORD=change-me

# Docker Compose
MYSQL_IMAGE_TAG=8.4
MYSQL_CONTAINER_NAME=we-ai-mysql
NGINX_IMAGE_TAG=stable-alpine
NGINX_CONTAINER_NAME=we-ai-nginx
NGINX_PORT=80
MINIO_IMAGE_TAG=latest
MINIO_MC_IMAGE_TAG=latest
MINIO_CONTAINER_NAME=we-ai-minio
MINIO_INIT_CONTAINER_NAME=we-ai-minio-init
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=change-me
MINIO_PUBLIC_BUCKET=we-ai-public
MINIO_PRIVATE_BUCKET=we-ai-private

# Dev bootstrap admin account
APP_AUTH_USERNAME=admin
APP_AUTH_PASSWORD=change-me

# JWT
JWT_SECRET=change-this-development-secret-key-at-least-32-bytes
JWT_ISSUER=we-ai-server
JWT_ACCESS_TOKEN_EXPIRATION=PT1H
JWT_REFRESH_TOKEN_EXPIRATION=P7D
```

## 로컬 실행 방법

PowerShell 기준입니다.

1. 프로젝트 루트로 이동

```powershell
cd C:\Users\0122k\IdeaProjects\we-ai-server
```

2. Docker Desktop 실행

- 좌하단 상태가 `Engine running`인지 확인합니다.

3. Docker 개발 스택 실행

```powershell
docker compose up -d
```

실행되는 서비스:

- `mysql`
- `minio`
- `nginx`

4. `dev` 프로필로 애플리케이션 실행

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\gradlew.bat bootRun
```

5. 실행 확인

- Backend direct Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Backend direct OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Backend direct Health Check: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
- nginx entry page: [http://localhost](http://localhost)
- nginx proxied Swagger UI: [http://localhost/swagger-ui.html](http://localhost/swagger-ui.html)
- nginx proxied Health Check: [http://localhost/api/v1/health](http://localhost/api/v1/health)
- MinIO API direct: [http://localhost:9000](http://localhost:9000)
- MinIO Console direct: [http://localhost:9001](http://localhost:9001)
- MinIO Console via nginx: [http://localhost/minio/](http://localhost/minio/)

## Docker 스택 구성

이 프로젝트의 `compose.yaml`은 아래 3개 서비스를 개발용으로 띄웁니다.

- `mysql`
  - Spring Boot 애플리케이션이 사용하는 기본 개발용 DB
- `minio`
  - S3 호환 오브젝트 스토리지
  - API 포트 `9000`, 콘솔 포트 `9001`
- `minio-init`
  - MinIO 초기화용 one-shot 컨테이너
  - 버킷 자동 생성 및 public 버킷 공개 설정
- `nginx`
  - 로컬 진입점 역할
  - Spring Boot를 `host.docker.internal:8080`으로 프록시
  - MinIO 콘솔과 API도 함께 프록시

nginx 경로 기준:

- `/swagger-ui.html`
- `/api/...`
- `/actuator/...`
- `/v3/api-docs/...`
- `/minio/`
- `/minio-api/`

## Swagger에서 테스트할 수 있는 API

### 공개 API

- `GET /api/v1/health`
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### 인증 필요 API

- `GET /api/v1/users/me`

### 관리자 전용 API

- `GET /api/v1/admin/users`
- `GET /api/v1/admin/users/{userId}`

## Swagger 테스트 예시

### 1. 회원가입

`POST /api/v1/auth/signup`

```json
{
  "username": "honggildong",
  "name": "홍길동",
  "email": "hong@example.com",
  "password": "password1234!"
}
```

### 2. 로그인

`POST /api/v1/auth/login`

```json
{
  "username": "honggildong",
  "password": "password1234!"
}
```

응답으로 `accessToken`, `refreshToken`을 받습니다.

### 3. 내 정보 조회

Swagger 우측 상단 `Authorize`에 access token을 넣은 뒤 아래 API를 호출합니다.

- `GET /api/v1/users/me`

### 4. access token 재발급

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "발급받은 refresh token"
}
```

### 5. 로그아웃

`POST /api/v1/auth/logout`

```json
{
  "refreshToken": "현재 refresh token"
}
```

## MySQL Workbench 연결 방법

Docker Compose로 MySQL이 실행 중이라면 아래 값으로 연결할 수 있습니다.

- Hostname: `127.0.0.1`
- Port: `.env`의 `DB_PORT`
- Username: `.env`의 `DB_USERNAME`
- Password: `.env`의 `DB_PASSWORD`
- Default Schema: `.env`의 `DB_NAME`

연결 후 `we_ai` 스키마를 열면 개발 실행 시점에 JPA가 생성한 테이블을 확인할 수 있습니다.

- `users`
- `refresh_tokens`

## MinIO 접속 정보

MinIO는 아래 두 방식으로 접근할 수 있습니다.

- Direct API: [http://localhost:9000](http://localhost:9000)
- Direct Console: [http://localhost:9001](http://localhost:9001)
- nginx Console proxy: [http://localhost/minio/](http://localhost/minio/)
- nginx API proxy: [http://localhost/minio-api/](http://localhost/minio-api/)

기본 계정은 `.env` 기준입니다.

- Access Key: `MINIO_ROOT_USER`
- Secret Key: `MINIO_ROOT_PASSWORD`

자동 생성되는 기본 버킷:

- `MINIO_PUBLIC_BUCKET`
- `MINIO_PRIVATE_BUCKET`

기본값 기준으로는 아래 두 버킷이 자동 생성됩니다.

- `we-ai-public`
- `we-ai-private`

추가로 `we-ai-public` 버킷은 읽기 공개로 설정됩니다.

추후 Spring Boot에서 파일 업로드 기능을 붙일 때는 이 MinIO를 S3 대체 스토리지로 사용할 수 있습니다.

주의:

- 기본 프로필에서는 `ddl-auto: none`이라 테이블이 자동 생성되지 않습니다.
- 로컬에서 테이블을 자동 생성하려면 `dev` 프로필로 실행해야 합니다.

## 테스트 실행

```powershell
.\gradlew.bat test --no-daemon --console=plain --rerun-tasks
```

현재 테스트에는 아래 항목이 포함됩니다.

- 회원가입 / 로그인 / 내 정보 조회
- refresh token 재발급 / 로그아웃
- 관리자 권한 접근 제어
- 에러 핸들링
- Swagger 문서 노출

## 프로젝트 구조

```text
src/main/java/com/weai/server
├─ domain
│  ├─ auth
│  ├─ health
│  └─ user
└─ global
   ├─ config
   ├─ dto
   ├─ entity
   ├─ error
   ├─ exception
   ├─ logging
   └─ security
```

## 참고

- 로컬 개발용 기본 관리자 계정은 `dev`와 `test` 프로필에서만 생성됩니다.
- 팀 공용 기본 프로필은 운영 안전성을 우선하도록 설정했습니다.
- refresh token은 DB에 원문이 아니라 해시로 저장됩니다.
