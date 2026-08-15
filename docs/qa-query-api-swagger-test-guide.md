# QA 조회 API 로컬 Swagger 테스트 방법

## 1. 서버 실행과 로그인

1. 로컬 서버를 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Windows PowerShell에서는 아래 명령도 사용할 수 있습니다.

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; ./gradlew.bat bootRun
```

2. Swagger UI에 접속합니다.

```text
http://localhost:8080/swagger-ui/index.html
```

3. `POST /api/v1/auth/signup` 또는 `POST /api/v1/auth/login`으로 토큰을 발급받습니다.

4. Swagger 우측 상단 `Authorize` 버튼에 아래 형식으로 입력합니다.

```text
Bearer {accessToken}
```

## 2. 프로젝트 준비

1. `POST /api/v1/projects`로 테스트 프로젝트를 생성합니다.

```json
{
  "projectName": "QA Query API Test",
  "description": "QA query API swagger test project",
  "localPath": "C:/Users/0122k/IdeaProjects/we-ai-server",
  "department": "BACKEND",
  "deadlineDate": "2026-12-31"
}
```

2. 응답의 `data.projectId`를 이후 `{projectId}`로 사용합니다.

3. 필요하면 `POST /api/v1/projects/{projectId}/tech-stacks`로 Gradle 빌드 도구를 등록합니다.

```json
{
  "name": "Gradle",
  "version": "9.4.1",
  "category": "BUILD_TOOL",
  "isRequired": true
}
```

## 3. 바로 테스트 가능한 조회 API

### 빌드 태스크 목록 조회

```text
GET /api/v1/projects/{projectId}/build/tasks
```

성공하면 `BUILD_TASK_LIST_SUCCESS`와 함께 `bootRun`, `build`, `test`, `clean`, `dependencies`, `bootJar`, `check` 목록이 반환됩니다.

### 프로파일별 실행 명령 조회

전체 프로파일:

```text
GET /api/v1/projects/{projectId}/environment/run-commands
```

특정 프로파일:

```text
GET /api/v1/projects/{projectId}/environment/run-commands?profile=dev
```

지원 프로파일은 `local`, `dev`, `test`, `prod`입니다.

### QA 리포트 목록 조회

샘플 QA 데이터가 없어도 빈 배열로 성공 응답을 확인할 수 있습니다.

```text
GET /api/v1/projects/{projectId}/qa/reports?page=0&size=20
GET /api/v1/projects/{projectId}/qa/reports?status=SUCCESS
GET /api/v1/projects/{projectId}/qa/reports?commitId={commitHash}
```

## 4. QA 샘플 데이터가 필요한 API

아래 3개 API는 저장된 QA 실행/리포트 데이터가 있어야 의미 있는 응답을 확인할 수 있습니다.

```text
GET /api/v1/projects/{projectId}/qa/runs/{qaRunId}/status
GET /api/v1/projects/{projectId}/qa/reports/{qaReportId}
GET /api/v1/projects/{projectId}/commits/{commitId}/qa
```

로컬 DB에 직접 샘플 데이터를 넣어 테스트할 수 있습니다. `{projectId}`와 `{commitHash}`는 실제 값으로 바꿔 넣습니다. `{commitHash}`는 `git log --oneline -1`로 확인할 수 있습니다.

```sql
INSERT INTO qa_runs (
  created_at,
  updated_at,
  project_id,
  commit_id,
  status,
  progress_rate,
  current_step,
  total_step,
  started_at,
  finished_at,
  error_message
) VALUES (
  NOW(6),
  NOW(6),
  {projectId},
  '{commitHash}',
  'SUCCESS',
  100,
  5,
  5,
  NOW(6),
  NOW(6),
  NULL
);

SET @qa_run_id = LAST_INSERT_ID();

INSERT INTO qa_reports (
  created_at,
  updated_at,
  project_id,
  qa_run_id,
  commit_id,
  commit_hash,
  commit_message,
  status,
  summary,
  total_issue_count,
  critical_count,
  major_count,
  minor_count,
  test_pass_count,
  test_fail_count
) VALUES (
  NOW(6),
  NOW(6),
  {projectId},
  @qa_run_id,
  '{commitHash}',
  '{commitHash}',
  'feat: QA 조회 API 테스트',
  'SUCCESS',
  '전체적으로 정상이며 일부 개선사항이 발견되었습니다.',
  1,
  0,
  1,
  0,
  3,
  0
);

SET @qa_report_id = LAST_INSERT_ID();

INSERT INTO qa_report_issues (
  created_at,
  updated_at,
  qa_report_id,
  severity,
  title,
  description,
  file_path,
  line_number,
  suggestion,
  status
) VALUES (
  NOW(6),
  NOW(6),
  @qa_report_id,
  'MAJOR',
  '예외 처리 확인 필요',
  '조회 API의 not found 응답을 확인하세요.',
  'QaQueryService.java',
  42,
  '도메인별 ErrorCode를 반환하는지 확인하세요.',
  'OPEN'
);

INSERT INTO qa_report_test_results (
  created_at,
  updated_at,
  qa_report_id,
  test_name,
  test_type,
  status,
  message,
  duration_ms
) VALUES (
  NOW(6),
  NOW(6),
  @qa_report_id,
  'QA 조회 API 테스트',
  'INTEGRATION',
  'PASSED',
  '정상 통과',
  120
);
```

그 다음 아래 값으로 Swagger에서 호출합니다.

```text
qaRunId = @qa_run_id
qaReportId = @qa_report_id
commitId = {commitHash}
```

## 5. 주의할 응답

- 프로젝트 멤버가 아니면 `PROJECT_ACCESS_DENIED`가 반환됩니다.
- 프로젝트가 `ACTIVE`가 아니면 `PROJECT_NOT_ACTIVE`가 반환됩니다.
- 잘못된 QA report status는 `INVALID_QA_REPORT_STATUS`가 반환됩니다.
- 잘못된 profile은 `INVALID_SPRING_PROFILE`이 반환됩니다.
- 커밋별 QA 조회는 로컬 git 저장소에서 커밋 존재 여부를 확인하므로 프로젝트 `localPath`가 git 저장소를 가리켜야 합니다.
