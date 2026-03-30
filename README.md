# we-ai-server

Spring Boot 기반 백엔드 보일러플레이트입니다. 공통 API 응답 포맷, 전역 예외 처리, JPA 감사 필드, 헬스체크 API, 로컬 MySQL 실행용 Docker Compose를 포함합니다.



### 권장 개발 환경

- Java 17
- Spring Boot 4.0.x
- Gradle Wrapper
- MySQL 8.4
- Docker Desktop
- IntelliJ IDEA



### Swagger에서 바로 테스트해볼 샘플 API

- `GET /api/v1/users`
- `GET /api/v1/users/1`
- `POST /api/v1/users`

`POST /api/v1/users` 예시 본문:

```json
{
  "name": "코딩싫어",
  "email": "coding@example.com"
}
```

### 프로젝트 구조
