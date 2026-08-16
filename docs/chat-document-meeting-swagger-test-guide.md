# 채팅 문서/회의 API 로컬 Swagger 테스트 방법

## 1. 서버 실행

PowerShell에서 실행합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; ./gradlew.bat bootRun
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

로그인 후 우측 상단 `Authorize`에 아래 형식으로 토큰을 넣습니다.

```text
Bearer {accessToken}
```

## 2. 준비값

기존 프로젝트를 사용하거나 새 프로젝트를 생성합니다.

```text
projectId = 16
```

문서 브리핑은 `txt`, `md`, `pdf`, `docx`, `pptx`에서 텍스트를 추출할 수 있습니다. 이미지로만 구성된 스캔 PDF는 OCR을 지원하지 않으므로 추출할 텍스트가 없을 수 있습니다. 구형 바이너리 형식인 `doc`, `ppt`는 업로드만 가능하며 브리핑을 생성하려면 `docx`, `pptx`로 변환해야 합니다.

예시 `briefing-test.md`:

```markdown
# 채팅 문서 회의 기능

문서 업로드 API 구현이 필요합니다.
문서 브리핑 생성 API를 추가합니다.
회의 모드 시작과 회의록 저장 기능을 확인해야 합니다.
PDF, DOCX, PPTX 본문에서도 요약, 핵심 포인트, 액션 아이템과 위험 요소를 생성합니다.
```

## 3. 문서 업로드

Swagger 태그:

```text
Chat Document
```

API:

```text
POST /api/v1/projects/{projectId}/chat/documents
```

입력:

```text
projectId = 16
file = briefing-test.md
description = 채팅 문서 브리핑 테스트
```

성공 코드:

```text
DOCUMENT_UPLOAD_SUCCESS
```

응답에서 `data.documentId`를 저장합니다.

## 4. 문서 브리핑 생성

API:

```text
POST /api/v1/projects/{projectId}/chat/documents/{documentId}/briefing
```

입력:

```text
projectId = 16
documentId = 문서 업로드 응답의 documentId
```

성공 코드:

```text
DOCUMENT_BRIEFING_CREATE_SUCCESS
```

`summary`, `keyPoints`, `actionItems`, `risks`, `keywords`가 반환되면 정상입니다.

주의:

- `pdf`, `docx`, `pptx`도 텍스트가 포함된 문서라면 브리핑을 생성할 수 있습니다.
- 이미지로만 된 PDF 또는 빈 문서는 `DOCUMENT_TEXT_NOT_EXTRACTED`가 반환될 수 있습니다.
- 구형 `doc`, `ppt`는 업로드 후 브리핑 생성 시 `DOCUMENT_TEXT_NOT_EXTRACTED`가 반환됩니다.
- 같은 문서에 이미 최신 브리핑이 있으면 새로 만들지 않고 기존 최신 브리핑을 반환합니다.

## 5. 문서 브리핑 목록 조회

API:

```text
GET /api/v1/projects/{projectId}/chat/document-briefings?page=0&size=20
GET /api/v1/projects/{projectId}/chat/document-briefings?status=COMPLETED
GET /api/v1/projects/{projectId}/chat/document-briefings?keyword=문서
```

성공 코드:

```text
DOCUMENT_BRIEFING_LIST_SUCCESS
```

방금 생성한 브리핑이 `briefings` 배열에 보이면 정상입니다.

## 6. 회의 모드 시작

Swagger 태그:

```text
Chat Meeting
```

API:

```text
POST /api/v1/projects/{projectId}/chat/meetings/start
```

Request body:

```json
{
  "title": "백엔드 API 회의",
  "description": "채팅 문서 및 회의록 기능 논의",
  "chatRoomId": null
}
```

성공 코드:

```text
MEETING_START_SUCCESS
```

응답에서 `data.meetingId`를 저장합니다.

주의:

- 같은 프로젝트에서 이미 `IN_PROGRESS` 회의가 있으면 `MEETING_ALREADY_IN_PROGRESS`가 반환됩니다.
- `chatRoomId`를 넣는 경우 해당 프로젝트의 활성 채팅방 ID여야 합니다.

## 7. 회의 종료 및 회의록 저장

API:

```text
POST /api/v1/projects/{projectId}/chat/meetings/{meetingId}/end
```

Request body:

```json
{
  "content": "오늘 회의에서는 문서 업로드 API와 문서 브리핑 생성 API, 회의록 저장 API 구현 범위를 정리하였다.",
  "summary": "채팅 문서/회의 기능 구현 범위를 확정하였다.",
  "actionItems": [
    "문서 업로드 API 구현",
    "문서 브리핑 생성 API 구현",
    "회의록 목록 조회 API 구현"
  ],
  "participants": []
}
```

성공 코드:

```text
MEETING_END_AND_MINUTE_SAVE_SUCCESS
```

`minuteId`, `status=ENDED`, `endedAt`이 반환되면 정상입니다.

주의:

- 이미 종료된 회의를 다시 종료하면 `MEETING_ALREADY_ENDED`가 반환됩니다.
- `participants`에 userId를 넣는 경우 모두 해당 프로젝트의 ACTIVE 멤버여야 합니다.

## 8. 회의록 목록 조회

API:

```text
GET /api/v1/projects/{projectId}/chat/meetings/minutes?page=0&size=20
GET /api/v1/projects/{projectId}/chat/meetings/minutes?keyword=백엔드
GET /api/v1/projects/{projectId}/chat/meetings/minutes?startDate=2026-08-16&endDate=2026-08-16
```

성공 코드:

```text
MEETING_MINUTE_LIST_SUCCESS
```

방금 저장한 회의록이 `minutes` 배열에 보이면 정상입니다.

## 9. 빠른 성공 확인 체크리스트

- `DOCUMENT_UPLOAD_SUCCESS`
- `DOCUMENT_BRIEFING_CREATE_SUCCESS`
- `DOCUMENT_BRIEFING_LIST_SUCCESS`
- `MEETING_START_SUCCESS`
- `MEETING_END_AND_MINUTE_SAVE_SUCCESS`
- `MEETING_MINUTE_LIST_SUCCESS`

위 6개 코드가 모두 `status=200`으로 찍히면 이번 API 기본 플로우는 정상입니다.
