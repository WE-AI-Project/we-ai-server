# we-ai-server

`we-ai-server`는 졸업작품 `ProjectHub`의 백엔드 서버입니다.  
현재는 인증/인가, 공통 응답, 예외 처리, 요청·응답 로깅, Swagger 문서화, Docker 기반 로컬 인프라를 중심으로 한 **백엔드 보일러플레이트**가 구축되어 있습니다.


---

## 1. 프로젝트 개요

기획/화면 설계 기준으로 현재 서비스는 다음 흐름을 중심으로 구성됩니다.

- 로그인 / 회원가입
- 이메일 인증 및 약관 동의
- 프로젝트 생성
- 6자리 코드 기반 프로젝트 참여
- 프로젝트 메인 대시보드
- My Tasks
- Notifications
- AI Insights
- Project QA Reports
- Shared Library
- Settings

추가로 기획서상 포함된 협업 기능 범위는 아래와 같습니다.

- 채팅방
- 파일 업데이트 추적
- AI 기반 QA 보조
- 진행률 분석
- 일정/캘린더 관련 기능
- 회의/요약 관련 기능

즉, 현재 백엔드는 단순한 로그인 서버가 아니라 **프로젝트 협업/QA/AI 보조형 서비스**를 목표로 확장될 예정입니다.

---

## 2. 현재 구현 상태

현재 실제 코드에 반영된 범위는 아래와 같습니다.

### 구현 완료

- 공통 API 응답 포맷 `ApiResponse<T>`
- 전역 예외 처리 및 에러 코드 체계
- 요청/응답 로깅 필터
- JWT Access Token 인증
- Refresh Token 저장/재발급/로그아웃
- Refresh Token 해시 저장
- Spring Security 기반 권한 분리
- DB 기반 회원가입 / 로그인
- 관리자 전용 사용자 조회 API
- Swagger / OpenAPI 문서화
- Docker 기반 로컬 스택
  - MySQL
  - MinIO
  - nginx
- MinIO 버킷 자동 생성
- Flyway 기반 DB 마이그레이션
- `dev / stag / prod` 프로필 분리

### 아직 구현 전 또는 부분 구현

- 프로젝트 생성 / 참여
- 프로젝트 코드 발급 / 참여 코드 검증
- 대시보드 데이터 집계
- Task 관리
- Notification 관리
- Project QA Report 도메인
- Shared Library 도메인
- 채팅 / 파일 업데이트 / 진행률 분석 / 캘린더

즉, **현재 저장소는 프로젝트 전체 도메인 중 “인증/보안/공통 인프라/운영 뼈대”가 먼저 구축된 상태**입니다.

---

## 3. 기획서·화면 설계서 기준 권장 도메인

기획 문서를 기준으로, 이후 백엔드 도메인은 아래처럼 확장하는 것을 권장합니다.

- `auth`
  - 로그인, 회원가입, 이메일 인증, 토큰 관리
- `project`
  - 프로젝트 생성, 수정, 조회, 참여 코드 발급
- `project-member`
  - 프로젝트 참여, 멤버 권한, 초대/참여 승인
- `task`
  - 개인/팀 작업 관리
- `notification`
  - 알림 목록, 읽음 처리, 푸시 알림 설정
- `qa`
  - QA 요청, QA 결과, AI QA 보조
- `report`
  - 프로젝트 리포트, 진행률 분석
- `chat`
  - 채팅방, 메시지, 회의 요약
- `file-update`
  - 파일 변경 내역, 브랜치/업데이트 알림
- `shared-library`
  - 공유 자료실
- `calendar`
  - 마감일 / 일정 관리
- `settings`
  - 사용자/프로젝트 설정

이 구조는 [CODING_CONVENTION.md](C:/Users/0122k/IdeaProjects/we-ai-server/CODING_CONVENTION.md)의 도메인 중심 패키지 구조와 맞춰 확장하는 것을 전제로 합니다.

---

## 4. 기술 스택

- Java 17
- Spring Boot 4.0.x
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Flyway
- MySQL 8.4
- JWT (`jjwt`)
- Springdoc OpenAPI / Swagger UI
- Docker Compose
- MinIO
- nginx
- Gradle Wrapper

---

## 5. 실행 프로필 정책

### 공통 설정

- 파일: [application.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application.yml)
- 운영에 가까운 안전한 기본값을 둡니다.
- `ddl-auto: validate`
- Docker Compose 자동 실행 비활성화

### 로컬 개발

- 파일: [application-dev.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-dev.yml)
- 로컬 개발 전용 설정
- Docker Compose 자동 연동
- 개발용 bootstrap 관리자 계정
- 개발용 로그 포맷

### Staging

- 파일: [application-stag.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-stag.yml)
- 개발 서버 / 검증 서버용 설정

### Production

- 파일: [application-prod.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-prod.yml)
- 운영 환경용 설정

---

## 6. 환경 변수

프로젝트 루트의 `.env` 파일을 사용합니다.  
저장소에는 실제 비밀값 대신 [`.env.example`](C:/Users/0122k/IdeaProjects/we-ai-server/.env.example)만 커밋합니다.

로컬 개발 기준 예시:
## 7. 로컬 실행 방법

---

## 8. Docker 스택 구성

파일: [compose.yaml](C:/Users/0122k/IdeaProjects/we-ai-server/compose.yaml)

### 현재 포함 서비스

- `mysql`
  - 로컬 개발 DB
- `minio`
  - S3 호환 오브젝트 스토리지
- `minio-init`
  - 기본 버킷 자동 생성
- `nginx`
  - 로컬 진입점 / 프록시
- `app`
  - staging 배포용 Spring Boot 컨테이너 (`staging` profile 사용 시)

### 자동 생성 버킷

- `we-ai-public`
- `we-ai-private`

---

## 10. Swagger 테스트 흐름

### 1. 헬스체크

- `GET /api/v1/health`
- [V1__create_users_and_refresh_tokens.sql](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql)

원칙:

- 공통 설정은 `ddl-auto: validate`
- 스키마 변경은 Flyway로 관리
- 기존 마이그레이션 파일 수정 대신 새 버전 파일 추가

---

현재 테스트 범위:

- 회원가입 / 로그인 / 현재 사용자 조회
- Refresh Token 재발급 / 로그아웃
- 인증 실패 / 권한 실패
- 공통 에러 응답
- Swagger 문서 노출

---

## 14. 이후 우선 구현 추천
1. `project`
   - 프로젝트 생성
   - 프로젝트 참여 코드 생성
   - 프로젝트 참여
2. `project-member`
   - 프로젝트 멤버 관리
   - 권한 분리
3. `task`
   - My Tasks 목록 / 상태 변경
4. `notification`
   - 알림 목록 / 읽음 처리
5. `qa`
   - Project QA Reports / AI QA 흐름

즉, 인증 다음 단계는 **프로젝트 온보딩 → 프로젝트 멤버십 → 태스크/알림 → QA/AI 확장** 순으로 가는 것이 설계서 흐름과 가장 잘 맞습니다.
