# we-ai-server Coding Convention

이 문서는 `we-ai-server` 프로젝트에서 공통으로 따를 백엔드 코딩 컨벤션을 정의합니다.

첨부해주신 코딩 컨벤션 문서의 흐름을 바탕으로 작성했으며,  
현재 프로젝트 구조와 [기획서.pdf](C:/Users/0122k/Desktop/졸작/기획서.pdf), [화면 설계서.pdf](C:/Users/0122k/Desktop/졸작/화면%20설계서.pdf)의 도메인 확장 방향을 함께 반영했습니다.

---

## 1. 공통 원칙

- **가독성 우선**: 짧은 최적화보다 읽기 쉬운 코드와 명확한 이름을 우선합니다.
- **일관성 유지**: 새 코드는 기존 프로젝트 스타일에 맞춥니다.
- **단일 책임 원칙(SRP)**: 클래스와 메서드는 하나의 역할에 집중합니다.
- **도메인 중심 설계**: 화면명보다 비즈니스 도메인 기준으로 패키지를 나눕니다.
- **명시적 상태 변경**: 엔티티 상태 변경은 의도가 드러나는 메서드로 처리합니다.

---

## 2. 프로젝트 구조

### (1) 전체 디렉터리 구조

```text
src/main/java/com/weai/server/
├── global/
│   ├── config/
│   ├── dto/
│   ├── entity/
│   ├── error/
│   ├── exception/
│   ├── logging/
│   ├── security/
│   └── swagger/
│
├── domain/
│   ├── auth/
│   ├── health/
│   ├── user/
│   └── {future-domain}/
│       ├── controller/
│       ├── domain/
│       ├── repository/
│       ├── request/
│       ├── response/
│       └── service/
└── WeAiServerApplication.java
```

### (2) 패키지별 역할

| 패키지 | 역할 |
| --- | --- |
| `controller` | HTTP 요청/응답 처리, 입력 검증, 서비스 호출 |
| `service` | 비즈니스 로직, 트랜잭션 경계 |
| `repository` | JPA 기반 영속성 처리 |
| `domain` | 엔티티, 도메인 상태, 도메인 규칙 |
| `request` | 요청 DTO |
| `response` | 응답 DTO |
| `global` | 전역 설정, 공통 응답, 예외, 보안, 로깅, Swagger |

### (3) 미래 도메인 확장 기준

기획/설계서 기준으로 이후 아래 도메인들이 추가될 가능성이 높습니다.

- `project`
- `project-member`
- `task`
- `notification`
- `qa`
- `report`
- `chat`
- `file-update`
- `shared-library`
- `calendar`
- `settings`

새 기능은 화면 단위가 아니라 위와 같은 **도메인 단위 패키지**로 추가합니다.

예:

- `screen.dashboard` 사용 금지
- `page.login` 사용 금지
- `domain.project`, `domain.task`, `domain.notification` 사용

---

## 3. 패키지명 / 클래스명 / 메서드명 규칙

### (1) 패키지명

- 소문자만 사용합니다.
- 도메인/기능 기준 명사를 사용합니다.
- 예:
  - `domain.auth.controller`
  - `global.security.jwt`

### (2) 클래스명

- PascalCase를 사용합니다.
- 역할이 드러나도록 명명합니다.
- 예:
  - `AuthController`
  - `UserService`
  - `JwtTokenProvider`
  - `ProjectRepository`

### (3) 메서드명

- camelCase를 사용합니다.
- 동사 + 목적어 형태를 사용합니다.
- 예:
  - `signUp`
  - `login`
  - `refresh`
  - `findById`
  - `registerUser`
  - `createProject`
  - `joinProject`

### (4) 변수명 / 상수명

- 변수명: camelCase
- 상수명: `UPPER_SNAKE_CASE`
- boolean은 `is`, `has`, `can` 계열을 우선 사용합니다.

---

## 4. Controller 규칙

### (1) 기본 원칙

- Controller는 HTTP 계층에만 집중합니다.
- 비즈니스 로직은 Service에 위임합니다.
- 응답은 항상 `ApiResponse<T>`로 감쌉니다.
- Swagger 문서화를 기본으로 합니다.

### (2) URL 규칙

- 기본 경로는 `/api/v1/...` 형태를 사용합니다.
- 리소스는 복수형 명사를 사용합니다.
- 권한이 다른 API는 경로로 분리합니다.
  - 일반 사용자: `/api/v1/users/...`
  - 관리자: `/api/v1/admin/users/...`

### (3) 액션성 엔드포인트 규칙

기획서 기준으로 다음과 같은 **행위 중심 기능**이 존재합니다.

- 로그인
- 회원가입
- 이메일 인증
- 토큰 재발급
- 프로젝트 참여 코드로 참여
- QA 실행
- 리포트 생성

이런 경우에는 **POST + 명확한 액션 경로**를 허용합니다.

예:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/projects/join`
- `POST /api/v1/qa/reports/generate`

### (4) Controller 분리 기준

- 사용 주체가 다르면 Controller를 분리합니다.
- 예:
  - `AuthController`
  - `UserController`
  - `AdminUserController`

---

## 5. API 설계 규칙

### (1) HTTP Method 사용 원칙

| Method | 용도 |
| --- | --- |
| `GET` | 조회 |
| `POST` | 생성, 로그인, 참여, 실행, 발급 |
| `PUT` | 전체 수정 |
| `PATCH` | 부분 수정 |
| `DELETE` | 삭제 |

### (2) PathVariable / Query Param / Body 사용 원칙

- 식별자는 `PathVariable`을 사용합니다.
- 목록 조회와 검색 조건은 `Query Param`을 사용합니다.
- 생성/수정/로그인/가입/참여는 `RequestBody`를 사용합니다.

### (3) 페이징 규칙

- 목록 조회는 `Pageable`을 우선 사용합니다.
- 응답은 `PageResponse<T>`를 사용합니다.
- 기본 정렬이 없으면 서비스 계층에서 안전한 기본 정렬을 지정합니다.

```java
@GetMapping
public ApiResponse<PageResponse<UserResponse>> getUsers(
    @ParameterObject
    @PageableDefault(size = 20, sort = "id")
    Pageable pageable
) {
    return ApiResponse.success(userService.findAll(pageable));
}
```

---

## 6. DTO 규칙

### (1) Request DTO

- `request` 패키지에 둡니다.
- 가능하면 `record`를 사용합니다.
- Bean Validation과 Swagger `@Schema`를 함께 작성합니다.

```java
public record SignUpRequest(
    @NotBlank String username,
    @NotBlank String name,
    @Email String email,
    @NotBlank String password
) {
}
```

### (2) Response DTO

- `response` 패키지에 둡니다.
- 엔티티를 외부로 직접 노출하지 않습니다.
- 정적 팩토리 메서드 `from(...)`을 우선 사용합니다.

```java
public record UserResponse(
    Long id,
    String username,
    String name,
    String email,
    UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getRole()
        );
    }
}
```

### (3) 화면 설계서 기반 요청 DTO 네이밍

화면 설계서에 등장하는 기능 특성상 아래 요청 객체 네이밍을 권장합니다.

- `LoginRequest`
- `SignUpRequest`
- `RefreshTokenRequest`
- `JoinProjectRequest`
- `CreateProjectRequest`
- `UpdateDeadlineRequest`
- `GenerateQaReportRequest`

즉, 화면 이름이 아니라 **유스케이스 기준 Request 이름**을 사용합니다.

---

## 7. Service 규칙

### (1) 기본 원칙

- Service는 유스케이스 단위의 비즈니스 로직을 담당합니다.
- 클래스에는 `@Transactional(readOnly = true)`를 두고, 쓰기 메서드에만 `@Transactional`을 추가합니다.
- Controller에는 Entity를 직접 반환하지 않습니다.

### (2) 메서드 구성 원칙

- 공개 메서드는 유스케이스 단위로 작성합니다.
- 검증/중복체크/정규화 로직은 private 메서드로 분리합니다.
- 조회 메서드는 목적이 드러나도록 `findBy...`, `get...`, `load...` 계열을 사용합니다.

### (3) 예시

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse registerUser(SignUpRequest request) {
        validateDuplicateUser(request.username(), request.email());
        User createdUser = userRepository.save(...);
        return UserResponse.from(createdUser);
    }
}
```

---

## 8. Domain(Entity) 규칙

### (1) Entity 위치와 이름

- 엔티티는 `domain` 패키지에 둡니다.
- 클래스명은 단수형 명사를 사용합니다.
- 예:
  - `User`
  - `RefreshToken`
  - `Project`
  - `Task`

### (2) BaseEntity 사용

- 생성/수정 시간은 `BaseEntity`를 상속해 공통 관리합니다.
- 개별 엔티티에서 `createdAt`, `updatedAt`를 중복 선언하지 않습니다.

### (3) 엔티티 설계 원칙

- Setter는 두지 않습니다.
- 생성은 정적 팩토리 메서드 또는 Builder를 사용합니다.
- 상태 변경은 의도가 드러나는 메서드로 처리합니다.
- Enum은 `EnumType.STRING`으로 저장합니다.

### (4) 예시

```java
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    public static User create(...) {
        return User.builder()
            .username(...)
            .role(...)
            .build();
    }
}
```

---

## 9. Security / JWT 규칙

### (1) 인증/인가

- 인증/인가 규칙은 `SecurityConfig`에서 일관되게 관리합니다.
- 경로 기준 권한 분리를 기본으로 합니다.
  - `/api/v1/admin/**` → `ADMIN`
  - `/api/v1/users/**` → `USER`, `ADMIN`

### (2) JWT 규칙

- Access Token과 Refresh Token 만료 시간을 분리합니다.
- 설정 키는 아래를 사용합니다.
  - `spring.jwt.secret`
  - `spring.jwt.accessTokenExpiration`
  - `spring.jwt.refreshTokenExpiration`
- Access Token claim에는 최소 `email`, `role`을 포함합니다.

### (3) Refresh Token 규칙

- Refresh Token 원문은 DB에 저장하지 않습니다.
- SHA-256 해시값만 저장합니다.
- 로그인/재발급 시 기존 토큰을 회전합니다.

### (4) 개발용 관리자 계정

- bootstrap 관리자 계정은 `dev`, `test` 프로필에서만 생성합니다.
- 공통/운영 설정에는 기본 관리자 계정을 두지 않습니다.

---

## 10. 예외 처리 / 공통 응답 규칙

### (1) 예외 처리

- 비즈니스 예외는 `ApiException`으로 던집니다.
- 에러 코드는 `ErrorCode`로 관리합니다.
- 전역 예외 처리는 `GlobalExceptionHandler`에서 담당합니다.

### (2) 응답 형식

- 성공/실패 응답은 공통 포맷을 유지합니다.
- 성공/목록/페이지 응답 모두 `ApiResponse<T>`를 사용합니다.
- 페이지 응답은 `ApiResponse<PageResponse<T>>`를 사용합니다.

### (3) Swagger 에러 예시

- `@SwaggerErrorResponses`를 사용해 공통 에러 응답 예시를 자동 등록합니다.

---

## 11. 로깅 규칙

### (1) HTTP 로깅

- 요청/응답 로깅은 `HttpLoggingFilter`에서 일관되게 처리합니다.
- `X-Request-Id`를 생성해 추적 가능하게 만듭니다.
- 민감정보는 마스킹합니다.
  - `Authorization`
  - `password`
  - `token`
  - `refreshToken`
  - `Cookie`

### (2) 도메인 로깅

- 의미 있는 도메인 이벤트만 `info`로 남깁니다.
- STT, AI 분석, 파일 변경 추적, QA 결과 생성 같은 기능이 들어오더라도 원본 민감 데이터 전체를 로그에 남기지 않습니다.
- 추적이 필요하면 식별자 중심 로그를 남깁니다.

---

## 12. 설정 / 프로필 / 환경변수 규칙

### (1) 설정 파일 분리

| 파일 | 역할 |
| --- | --- |
| `application.yml` | 공통 설정 |
| `application-dev.yml` | 로컬 개발용 |
| `application-stag.yml` | 개발 서버 / staging |
| `application-prod.yml` | 운영 |

### (2) 기본 원칙

- 공통 설정에는 개발 편의 설정을 두지 않습니다.
- `dev`에서만 Docker Compose 자동 연동, 개발용 bootstrap 계정 등을 허용합니다.
- 실제 비밀값은 `.env`에서 읽고, 저장소에는 `.env.example`만 커밋합니다.

### (3) DDL / 마이그레이션 규칙

- 공통 설정은 `ddl-auto: validate`를 사용합니다.
- 스키마 변경은 Flyway로 관리합니다.
- 기존 마이그레이션을 수정하지 않고 새 버전 파일을 추가합니다.

---

## 13. 기획서 기반 API 설계 보강 규칙

기획서와 화면 설계서에서 다음 액션이 명확히 보입니다.

- 이메일 인증
- 프로젝트 생성
- 프로젝트 코드로 참여
- 마감일 설정
- QA 실행
- 알림 조회 / 읽음 처리

이런 기능은 아래 원칙을 따릅니다.

- 인증/검증/실행/참여처럼 행위 중심인 경우 `POST` 사용 가능
- 상태 변경이 명확한 경우 `PATCH` 사용
- 읽음 처리처럼 부분 상태 변경은 `PATCH /notifications/{id}/read` 형태 허용
- 코드 참여처럼 별도 리소스를 만들지 않는 경우 `POST /projects/join` 허용

예:

- `POST /api/v1/projects`
- `POST /api/v1/projects/join`
- `PATCH /api/v1/projects/{projectId}/deadline`
- `POST /api/v1/qa/reports/generate`
- `PATCH /api/v1/notifications/{notificationId}/read`

---

## 14. 테스트 규칙

- API 흐름은 통합 테스트로 검증합니다.
- 최소한 아래 시나리오는 테스트를 유지합니다.
  - 회원가입
  - 로그인
  - 토큰 재발급
  - 인증 실패
  - 권한 실패
  - 예외 응답 형식

- 프로젝트 생성/참여가 추가되면 아래도 테스트 대상에 포함합니다.
  - 프로젝트 생성 성공 / 실패
  - 코드 참여 성공 / 실패
  - 프로젝트 권한 검증

---

## 15. Lombok 사용 규칙

- 허용:
  - `@Getter`
  - `@RequiredArgsConstructor`
  - `@Builder`
  - `@Slf4j`
  - `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 지양:
  - `@Data`
  - 엔티티에 대한 무분별한 `@Setter`

---

## 16. 새 코드 작성 체크리스트

- 도메인 기준 패키지에 위치시켰는가
- Request / Response DTO를 분리했는가
- 화면명 대신 도메인명으로 패키지를 만들었는가
- Controller에서 비즈니스 로직을 직접 처리하지 않았는가
- 응답을 `ApiResponse<T>`로 감쌌는가
- 예외를 `ApiException` + `ErrorCode`로 처리했는가
- Swagger 설명과 에러 예시를 추가했는가
- 민감정보를 로그에 그대로 남기지 않는가
- DB 스키마 변경이 있다면 Flyway 마이그레이션을 추가했는가

---

## 17. 점진적 정리 원칙

- 새 코드는 반드시 이 문서를 따릅니다.
- 기존 코드가 일부 다른 구조를 사용하더라도, 기능 변경 시점에 함께 정리합니다.
- 특히 기획서상 도메인이 늘어날수록 화면 단위 패키지가 아닌 **도메인 단위 패키지**를 유지합니다.
