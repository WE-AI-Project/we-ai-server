# 로컬 Swagger 섹션별 상세 테스트 가이드

이 문서는 로컬 Swagger UI `http://localhost:8080/swagger-ui/index.html` 에서 자주 테스트하는 아래 4개 섹션만 따로 모아서 정리한 가이드입니다.

1. `Auth`
2. `프로젝트`
3. `프로젝트 대시보드`
4. `AI Chat / AI Debate / AI QA`

전체 API를 한 번에 보는 문서는 [local-swagger-api-test-guide.md](C:/Users/0122k/IdeaProjects/we-ai-server/docs/local-swagger-api-test-guide.md) 에 있습니다.

## 1. 공통 준비

### 1-1. 서버 실행 확인

아래 주소가 열려야 합니다.

- `http://localhost:8080/swagger-ui/index.html`

먼저 확인할 API:

- `GET /api/v1/health`

성공 기준:

- `200 OK`
- Swagger에서 응답이 바로 보임

### 1-2. Swagger에서 가장 많이 실수하는 부분

요청 본문은 반드시 아래처럼 `순수 JSON 객체`만 넣어야 합니다.

정상 예시:

```json
{
  "projectName": "Schedule API Test Project",
  "description": "Swagger test project",
  "localPath": "C:/WE_AI/schedule-api-test",
  "department": "BACKEND",
  "deadlineDate": "2026-06-30"
}
```

비정상 예시:

```json
"{\n  \"projectName\": \"Schedule API Test Project\" \n}"
```

위처럼 바깥 큰따옴표가 있거나 `\n`, `\"` 가 그대로 보이면 `400 Request body could not be parsed.` 가 납니다.

### 1-3. 먼저 저장할 값

테스트를 하면서 아래 값들을 메모해두면 이후 API를 바로 이어서 확인할 수 있습니다.

| 저장할 값 | 어디서 확인하는지 |
|---|---|
| `{accessToken}` | 로그인 성공 응답 |
| `{refreshToken}` | 로그인 성공 응답 |
| `{projectId}` | 프로젝트 생성 응답의 `data.projectId` |
| `{projectCode}` | 프로젝트 생성 응답의 `data.projectCode` |
| `{memberId}` | 멤버 목록 응답의 `data.members[].projectMemberId` |
| `{memberUserId}` | 멤버 목록 응답의 `data.members[].userId` |
| `{techStackId}` | 기술 스택 조회 응답의 `data.techStacks[].techStackId` |
| `{scheduleId}` | 일정 조회 응답의 `data.schedules[].scheduleId` |

중요:

- `assigneeId` 는 `projectMemberId`가 아닙니다.
- `assigneeId` 는 `users.id`, 즉 `members[].userId` 입니다.

## 2. Auth 섹션 상세 테스트

Auth는 아래 순서대로 테스트하면 가장 안정적입니다.

### 2-1. 회원가입

API:

- `POST /api/v1/auth/signup`

리더 계정 예시:

```json
{
  "username": "leader0707",
  "name": "리더사용자",
  "email": "leader0707@example.com",
  "password": "leader1234!"
}
```

멤버 계정 예시:

```json
{
  "username": "member0707",
  "name": "멤버사용자",
  "email": "member0707@example.com",
  "password": "member1234!"
}
```

성공 기준:

- `201` 또는 성공 응답
- 중복 이메일이 아니어야 함

자주 나는 오류:

- `409`: 이미 가입된 이메일
- `400`: 비밀번호 길이/이메일 형식 오류

### 2-2. 일반 로그인

API:

- `POST /api/v1/auth/login`

예시:

```json
{
  "email": "leader0707@example.com",
  "password": "leader1234!"
}
```

성공하면 아래를 복사해 둡니다.

- `accessToken`
- `refreshToken`

### 2-3. Swagger Authorize 설정

Swagger 우측 상단 `Authorize` 클릭 후 아래처럼 넣습니다.

```text
Bearer {accessToken}
```

이후 인증이 필요한 프로젝트/대시보드 API를 호출할 수 있습니다.

### 2-4. 내 정보 조회

API:

- `GET /api/v1/users/me`

성공 기준:

- 현재 로그인한 사용자 이름/이메일이 내려옴

이 단계가 성공하면 토큰 설정은 정상입니다.

### 2-5. 이메일 인증 코드 발송

API:

- `POST /api/v1/auth/email-login/code`

이메일 발송 예시:

```json
{
  "email": "leader0707@example.com",
  "deliveryChannel": "EMAIL"
}
```

카카오톡 발송 예시:

```json
{
  "email": "leader0707@example.com",
  "deliveryChannel": "KAKAO_TALK",
  "kakaoAuthorizationCode": "여기에_카카오_인가코드"
}
```

확인 포인트:

1. 응답에 `debugCode`가 있으면 그 값을 씁니다.
2. `debugCode`가 없으면 실제 이메일 또는 카카오톡으로 온 6자리 코드를 확인합니다.

### 2-6. 이메일 인증 코드 로그인

API:

- `POST /api/v1/auth/email-login`

예시:

```json
{
  "email": "leader0707@example.com",
  "verificationCode": "123456"
}
```

성공 기준:

- 토큰 재발급과 비슷하게 `accessToken`, `refreshToken`이 내려옴

### 2-7. 리프레시 토큰 재발급

API:

- `POST /api/v1/auth/refresh`

예시:

```json
{
  "refreshToken": "여기에_refreshToken"
}
```

성공 기준:

- 새 `accessToken`
- 새 `refreshToken`

### 2-8. 로그아웃

API:

- `POST /api/v1/auth/logout`

예시:

```json
{
  "refreshToken": "여기에_refreshToken"
}
```

성공 기준:

- 성공 메시지 반환

### 2-9. 소셜 로그인 URL 조회

조회 API:

- `GET /api/v1/auth/kakao/url`
- `GET /api/v1/auth/kakao/message-url`
- `GET /api/v1/auth/naver/url`
- `GET /api/v1/auth/google/url`

성공 기준:

- 응답에 소셜 로그인용 URL이 반환됨

### 2-10. 소셜 로그인 완료 API

카카오:

- `POST /api/v1/auth/kakao/login`

```json
{
  "code": "여기에_카카오_인가코드"
}
```

네이버:

- `POST /api/v1/auth/naver/login`

```json
{
  "code": "여기에_네이버_인가코드",
  "state": "여기에_네이버_state"
}
```

구글:

- `POST /api/v1/auth/google/login`

```json
{
  "code": "여기에_구글_인가코드"
}
```

성공 기준:

- 최종적으로 앱용 토큰이 발급됨

## 3. 프로젝트 섹션 상세 테스트

프로젝트 섹션은 사실상 아래 묶음까지 포함해서 보면 됩니다.

1. 프로젝트 생성/참여/상세
2. 멤버 조회/변경
3. 기술 스택 조회/추가/수정/삭제
4. 일정 조회/생성/수정/상태 변경/삭제

가장 추천하는 순서는 아래입니다.

## 3-1. 프로젝트 생성

API:

- `POST /api/v1/projects`

예시:

```json
{
  "projectName": "Schedule API Test Project",
  "description": "Swagger test project",
  "localPath": "C:/WE_AI/schedule-api-test",
  "department": "BACKEND",
  "deadlineDate": "2026-06-30"
}
```

성공 기준:

- `data.projectId` 생성
- `data.projectCode` 생성

반드시 저장:

- `{projectId}`
- `{projectCode}`

### 3-2. 내 프로젝트 목록 조회

API:

- `GET /api/v1/projects/my`

성공 기준:

- 방금 생성한 프로젝트가 목록에 있음

### 3-3. 프로젝트 상세 조회

API:

- `GET /api/v1/projects/{projectId}`

성공 기준:

- 프로젝트명, 설명, 상태 등 상세 정보 확인

### 3-4. 프로젝트 정보 수정

API:

- `PATCH /api/v1/projects/{projectId}`

예시:

```json
{
  "projectName": "Schedule API Test Project Updated",
  "description": "Swagger patch test project",
  "repositoryUrl": "https://github.com/WE-AI-Project/we-ai-server",
  "localPath": "C:/WE_AI/schedule-api-test",
  "startDate": "2026-06-01",
  "targetDate": "2026-06-30",
  "status": "ACTIVE"
}
```

성공 기준:

- 응답에서 변경된 값 확인
- 다시 상세 조회해도 값이 반영됨

### 3-5. 프로젝트 참여

이 API는 두 번째 계정으로 테스트하는 것을 추천합니다.

순서:

1. 멤버 계정으로 로그인
2. `Authorize`를 멤버 토큰으로 교체
3. 아래 API 실행

API:

- `POST /api/v1/projects/join`

예시:

```json
{
  "projectCode": "여기에_projectCode",
  "department": "FRONTEND"
}
```

성공 기준:

- 해당 멤버 계정이 프로젝트에 참여됨

그 다음 다시 리더 계정으로 로그인해서 `Authorize`를 리더 토큰으로 바꿉니다.

### 3-6. 멤버 목록 조회

API:

- `GET /api/v1/projects/{projectId}/members`

이 API는 매우 중요합니다.

여기서 확인할 값:

- `projectMemberId`
- `userId`
- `name`
- `role`
- `department`

반드시 메모:

- `{memberId}` = `projectMemberId`
- `{memberUserId}` = `userId`

### 3-7. 멤버 상세 조회

API:

- `GET /api/v1/projects/{projectId}/members/{memberId}`

성공 기준:

- 특정 멤버 상세 정보가 정상 조회됨

### 3-8. 멤버 역할 변경

API:

- `PATCH /api/v1/projects/{projectId}/members/{memberId}/role`

예시:

```json
{
  "role": "MEMBER"
}
```

성공 기준:

- 응답에서 역할 변경 확인
- 다시 멤버 상세 또는 멤버 목록 조회 시 역할 반영

주의:

- 리더 본인을 테스트 대상으로 쓰기보다, 참여한 멤버 계정을 대상으로 하는 것이 안전합니다.

### 3-9. 멤버 부서 변경

API:

- `PATCH /api/v1/projects/{projectId}/members/{memberId}/department`

예시:

```json
{
  "department": "QA"
}
```

성공 기준:

- 부서 값이 변경됨

### 3-10. 기술 스택 조회

API:

- `GET /api/v1/projects/{projectId}/tech-stacks`

성공 기준:

- 현재 등록된 기술 스택 목록 반환

### 3-11. 기술 스택 추가

API:

- `POST /api/v1/projects/{projectId}/tech-stacks`

예시:

```json
{
  "name": "Spring Boot",
  "version": "3.2.5",
  "category": "BACKEND",
  "isRequired": true
}
```

성공 기준:

- 기술 스택이 생성됨

반드시 저장:

- `{techStackId}`

### 3-12. 기술 스택 수정

API:

- `PATCH /api/v1/projects/{projectId}/tech-stacks/{techStackId}`

예시:

```json
{
  "name": "Spring Boot",
  "version": "3.3.0",
  "category": "BACKEND",
  "isRequired": true
}
```

성공 기준:

- 버전 또는 필드가 수정됨

### 3-13. 기술 스택 삭제

API:

- `DELETE /api/v1/projects/{projectId}/tech-stacks/{techStackId}`

성공 기준:

- 삭제 성공 응답
- 다시 기술 스택 조회 시 목록에서 사라짐

### 3-14. 일정 목록 조회

API:

- `GET /api/v1/projects/{projectId}/schedules`

성공 기준:

- 일정 목록이 반환됨

### 3-15. 일정 생성

API:

- `POST /api/v1/projects/{projectId}/schedules`

기본 예시:

```json
{
  "title": "프로젝트 일정 상세 API 구현",
  "description": "일정 상세 조회 API 개발",
  "department": "BACKEND",
  "startDate": "2026-05-24",
  "endDate": "2026-05-24",
  "priority": "HIGH",
  "status": "TODO"
}
```

담당자를 특정 멤버로 지정하는 예시:

```json
{
  "title": "프론트 화면 연결",
  "description": "멤버 계정 담당 일정",
  "assigneeId": 7,
  "department": "FRONTEND",
  "startDate": "2026-05-25",
  "endDate": "2026-05-26",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS"
}
```

중요:

- `assigneeId`에는 `members[].userId`를 넣습니다.
- `projectMemberId`를 넣으면 잘못된 값입니다.

성공 기준:

- `data.scheduleId` 생성

반드시 저장:

- `{scheduleId}`

### 3-16. 일정 상세 조회

API:

- `GET /api/v1/projects/{projectId}/schedules/{scheduleId}`

성공 기준:

- 일정 제목, 설명, 담당자, 상태, 우선순위 확인

### 3-17. 일정 수정

API:

- `PATCH /api/v1/projects/{projectId}/schedules/{scheduleId}`

예시:

```json
{
  "title": "프로젝트 일정 수정 API 구현",
  "description": "일정 수정 기능 개발",
  "assigneeId": 7,
  "department": "BACKEND",
  "startDate": "2026-05-24",
  "endDate": "2026-05-25",
  "priority": "HIGH",
  "status": "IN_PROGRESS"
}
```

성공 기준:

- 일정 상세 조회 시 변경 반영

### 3-18. 일정 상태만 변경

API:

- `PATCH /api/v1/projects/{projectId}/schedules/{scheduleId}/status`

예시:

```json
{
  "status": "DONE"
}
```

성공 기준:

- 상태만 변경됨

### 3-19. 일정 필터 조회

API:

- `GET /api/v1/projects/{projectId}/schedules/filter`

추천 예시:

1. 부서 필터
   - `/api/v1/projects/{projectId}/schedules/filter?department=BACKEND`
2. 부서 + 상태 필터
   - `/api/v1/projects/{projectId}/schedules/filter?department=BACKEND&status=TODO`
3. 기간 필터
   - `/api/v1/projects/{projectId}/schedules/filter?startDate=2026-05-24&endDate=2026-05-25`

성공 기준:

- 조건에 맞는 일정만 내려옴

### 3-20. 일정 삭제

API:

- `DELETE /api/v1/projects/{projectId}/schedules/{scheduleId}`

성공 기준:

- 삭제 성공 응답
- 다시 일정 목록 조회 시 사라짐

## 4. 프로젝트 대시보드 섹션 상세 테스트

프로젝트 대시보드 API는 데이터가 전혀 없으면 일부 값이 `0` 또는 빈 배열로 보일 수 있습니다.

그래서 아래 순서가 좋습니다.

1. 프로젝트 생성
2. 멤버 1명 이상 참여
3. 일정 1개 이상 생성
4. 기술 스택 1개 이상 추가
5. 그 다음 대시보드 API 테스트

### 4-1. 프로젝트 대시보드 요약

API:

- `GET /api/v1/projects/{projectId}/dashboard`

목적:

- 프로젝트 요약 정보 한 번에 확인

성공 기준:

- 프로젝트 정보
- 일정 진행 정보
- 부서별 요약 정보가 내려옴

### 4-2. 최근 활동 목록 조회

API:

- `GET /api/v1/projects/{projectId}/dashboard/activities`

추천 호출:

- `/api/v1/projects/{projectId}/dashboard/activities`
- `/api/v1/projects/{projectId}/dashboard/activities?limit=5`

확인 포인트:

- 프로젝트 생성
- 멤버 참여
- 일정 생성/수정
- 기술 스택 추가/수정

같은 작업을 한 뒤 다시 조회하면 활동이 늘어나는지 확인할 수 있습니다.

### 4-3. 진행률 통계 조회

API:

- `GET /api/v1/projects/{projectId}/dashboard/progress`

확인 포인트:

- `totalScheduleCount`
- `todoCount`
- `inProgressCount`
- `doneCount`
- `completedCount`
- `holdCount`
- `completedWorkCount`
- `remainingScheduleCount`
- `progressRate`

테스트 팁:

1. 일정 하나를 `TODO`로 생성
2. 진행률 조회
3. 그 일정 상태를 `DONE`으로 변경
4. 다시 진행률 조회

이렇게 하면 `progressRate`가 실제로 바뀌는지 쉽게 확인할 수 있습니다.

### 4-4. 마일스톤 목록 조회

API:

- `GET /api/v1/projects/{projectId}/dashboard/milestones`

상태 필터:

- `/api/v1/projects/{projectId}/dashboard/milestones?status=IN_PROGRESS`

주의:

- 마일스톤 생성 API는 현재 범위에 없으므로, 조회 전에 DB에 데이터가 있어야 합니다.

삽입 SQL:

```sql
INSERT INTO project_milestones (
  created_at,
  updated_at,
  project_id,
  title,
  description,
  start_date,
  end_date,
  status,
  progress_rate
) VALUES (
  NOW(6),
  NOW(6),
  {projectId},
  '1차 마일스톤',
  '핵심 API 구현 완료',
  '2026-06-09',
  '2026-06-16',
  'IN_PROGRESS',
  55
);
```

확인 SQL:

```sql
SELECT milestone_id, project_id, title, status, progress_rate, start_date, end_date
FROM project_milestones
WHERE project_id = {projectId}
ORDER BY start_date ASC;
```

성공 기준:

- 조회 결과에 방금 넣은 마일스톤이 보임

### 4-5. 부서별 현황 조회

API:

- `GET /api/v1/projects/{projectId}/dashboard/departments`

확인 포인트:

- `department`
- `memberCount`
- `scheduleCount`
- `todoCount`
- `inProgressCount`
- `completedScheduleCount`
- `holdCount`
- `progressRate`

테스트 팁:

1. BACKEND 멤버, FRONTEND 멤버를 각각 둡니다.
2. BACKEND 일정, FRONTEND 일정을 각각 하나씩 만듭니다.
3. 한쪽 일정만 `DONE` 처리합니다.
4. 부서별 현황 API를 호출하면 부서별 진행률 차이가 보여야 합니다.

## 5. AI Chat / Debate / QA 섹션 상세 테스트

AI 섹션은 일반 CRUD API와 다르게 로컬 AI 실행 환경이 준비되어 있어야 합니다.

### 5-1. AI API 실행 전 체크

먼저 확인할 것:

1. 백엔드 서버가 정상 실행 중인지
2. Ollama 또는 연결된 AI 모델 서버가 떠 있는지
3. `AI Chat`용 ChromaDB가 떠 있는지
4. 관련 `.env` 값이 들어가 있는지

환경이 준비되지 않으면 보통 아래처럼 실패합니다.

- `500 Internal Server Error`
- AI 모델 연결 실패
- ChromaDB 연결 실패

### 5-2. AI QA 테스트

API:

- `POST /api/v1/ai/qa`

목적:

- 코드 diff를 분석해서 위험 요소와 개선 포인트를 받는 API

예시:

```json
{
  "diff": "diff --git a/src/main/java/com/example/UserService.java b/src/main/java/com/example/UserService.java\n@@\n- return userRepository.findById(id).get();\n+ return userRepository.findById(id).orElse(null);\n"
}
```

성공 기준:

- diff 분석 결과가 응답에 내려옴

테스트 팁:

- 너무 긴 diff보다 짧은 diff로 먼저 성공 여부를 보는 것이 좋습니다.

### 5-3. AI Debate 테스트

API:

- `POST /api/v1/ai/debate`

목적:

- 하나의 질문에 대해 다각도로 분석한 결과를 받는 API

예시:

```json
{
  "query": "프로젝트 일정 관리 API를 더 안정적으로 운영하려면 어떤 구조가 좋을까요?"
}
```

성공 기준:

- 토론형 분석 결과가 반환됨

테스트 팁:

- 너무 짧은 질문보다, 개선 목적이 드러나는 질문이 응답 품질이 좋습니다.

### 5-4. AI Chat 테스트

API:

- `POST /api/v1/ai/chat`

목적:

- 프로젝트 문서나 지식 기반을 참고하는 질의응답 API

예시:

```json
{
  "question": "프로젝트에서 인증 코드는 어떤 방식으로 검증되나요?"
}
```

성공 기준:

- 질문에 대한 자연어 답변 반환

주의:

- 이 API는 RAG/ChromaDB 의존성이 있어서, AI QA나 Debate보다 환경 영향을 더 많이 받습니다.
- Chat만 실패하면 ChromaDB나 임베딩 스토어 설정을 먼저 의심하면 됩니다.

## 6. 섹션별 추천 테스트 순서

정말 실패 없이 따라가려면 아래 순서가 가장 좋습니다.

1. `GET /api/v1/health`
2. `POST /api/v1/auth/signup`
3. `POST /api/v1/auth/login`
4. `GET /api/v1/users/me`
5. `POST /api/v1/projects`
6. `GET /api/v1/projects/my`
7. `GET /api/v1/projects/{projectId}`
8. `PATCH /api/v1/projects/{projectId}`
9. 멤버 계정 로그인
10. `POST /api/v1/projects/join`
11. 리더 계정 재로그인
12. `GET /api/v1/projects/{projectId}/members`
13. `GET /api/v1/projects/{projectId}/members/{memberId}`
14. `PATCH /api/v1/projects/{projectId}/members/{memberId}/role`
15. `PATCH /api/v1/projects/{projectId}/members/{memberId}/department`
16. `POST /api/v1/projects/{projectId}/tech-stacks`
17. `GET /api/v1/projects/{projectId}/tech-stacks`
18. `PATCH /api/v1/projects/{projectId}/tech-stacks/{techStackId}`
19. `POST /api/v1/projects/{projectId}/schedules`
20. `GET /api/v1/projects/{projectId}/schedules`
21. `GET /api/v1/projects/{projectId}/schedules/{scheduleId}`
22. `PATCH /api/v1/projects/{projectId}/schedules/{scheduleId}`
23. `PATCH /api/v1/projects/{projectId}/schedules/{scheduleId}/status`
24. `GET /api/v1/projects/{projectId}/schedules/filter`
25. `GET /api/v1/projects/{projectId}/dashboard`
26. `GET /api/v1/projects/{projectId}/dashboard/activities`
27. `GET /api/v1/projects/{projectId}/dashboard/progress`
28. `GET /api/v1/projects/{projectId}/dashboard/departments`
29. `GET /api/v1/projects/{projectId}/dashboard/milestones`
30. 마지막으로 `AI QA -> AI Debate -> AI Chat`

## 7. 자주 막히는 문제

### 7-1. 400 Parse Error

원인:

- JSON 전체를 문자열로 넣음
- Windows 경로 역슬래시 문제

해결:

- 순수 JSON으로 다시 붙여 넣기
- 경로를 `C:/...` 로 바꾸기

### 7-2. 401 Unauthorized

원인:

- `Authorize`에 토큰이 없음
- 토큰이 만료됨

해결:

1. 다시 로그인
2. 새 토큰으로 `Authorize` 다시 입력

### 7-3. 403 PROJECT_ACCESS_DENIED

원인:

- 프로젝트 멤버가 아닌 계정

해결:

- 해당 계정으로 먼저 `POST /api/v1/projects/join`

### 7-4. assigneeId 오류

원인:

- `projectMemberId`를 넣음

해결:

- 반드시 `members[].userId` 값을 사용

### 7-5. 마일스톤 조회가 빈 배열

원인:

- `project_milestones` 테이블에 데이터가 없음

해결:

- 위 SQL로 먼저 데이터 삽입

### 7-6. AI Chat만 실패

원인:

- ChromaDB
- 임베딩 스토어
- RAG 설정 문제

해결:

- ChromaDB 실행 상태와 관련 환경 변수 점검
