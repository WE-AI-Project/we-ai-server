# we-ai-server

Spring Boot 기반 백엔드 보일러플레이트입니다. 공통 API 응답 포맷, 전역 예외 처리, JPA 감사 필드, 헬스체크 API, 로컬 MySQL 실행용 Docker Compose를 포함합니다.

### 실행 방법

```bash
1. git clone https://github.com/WE-AI-Project/we-ai-server.git
2. cd we-ai-server
3. 프로젝트 루트의 `.env` 값을 확인
4. docker compose up -d
5. ./gradlew bootRun
```

Windows PowerShell에서는 아래처럼 실행해도 됩니다.

```powershell
.\gradlew.bat bootRun
```

개발 로그 포맷과 DevTools를 함께 쓰려면 `dev` 프로필로 실행하세요.

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\gradlew.bat bootRun
```

IntelliJ에서는 `Run/Debug Configurations`에서 Active profiles를 `dev`로 지정하면 됩니다.

빌드와 테스트는 아래 명령으로 확인할 수 있습니다.

```bash
./gradlew clean build
./gradlew test
```

> **주의**: 기본 설정은 로컬 MySQL(`localhost:3306`)을 사용합니다. `compose.yaml`을 먼저 실행하거나, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경 변수를 맞춰 주세요.

### .env 사용 방식

- 프로젝트 루트의 `.env`를 Spring Boot와 Docker Compose가 함께 사용합니다.
- Spring Boot는 `spring.config.import`로 `.env`를 읽습니다.
- Docker Compose는 루트의 `.env`를 자동으로 읽어 `compose.yaml` 변수 치환에 사용합니다.
- 커밋 가능한 예시값은 `.env.example`에 있고, 실제 값은 `.env`에 둡니다.

### 권장 개발 환경

- Java 17
- Spring Boot 4.0.x
- Gradle Wrapper
- MySQL 8.4
- Docker Desktop
- IntelliJ IDEA

### 현재 포함된 보일러플레이트

- 공통 응답 객체: `ApiResponse`
- 전역 예외 처리: `GlobalExceptionHandler`
- 공통 에러 코드: `ErrorCode`
- 생성/수정 시간 관리용 `BaseEntity`
- JPA Auditing 설정
- 샘플 헬스체크 API: `GET /api/v1/health`
- 샘플 사용자 API: `GET /api/v1/users`, `GET /api/v1/users/{userId}`, `POST /api/v1/users`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- 개발용 콘솔 로그 포맷: `application-dev.yml`
- DevTools 핫리로드 지원
- 테스트용 H2 설정

### Swagger에서 바로 테스트해볼 샘플 API

- `GET /api/v1/users`
- `GET /api/v1/users/1`
- `POST /api/v1/users`

`POST /api/v1/users` 예시 본문:

```json
{
  "name": "김코딩",
  "email": "coding@example.com"
}
```

### 프로젝트 구조

```text
src
├─ main
│  ├─ java/com/weai/server
│  │  ├─ domain
│  │  │  └─ health
│  │  └─ global
│  │     ├─ config
│  │     ├─ dto
│  │     ├─ entity
│  │     ├─ error
│  │     └─ exception
│  └─ resources
│     └─ application.yml
└─ test
   ├─ java/com/weai/server
   └─ resources/application-test.yml
```

### 다음 추천 작업

- 사용자/도메인 엔티티 추가
- Security/JWT 인증 구조 추가
- CI/CD와 배포 환경 분리
