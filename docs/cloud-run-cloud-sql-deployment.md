# Cloud Run + Cloud SQL 배포 가이드

## 대상 아키텍처

- 프론트엔드: Vercel
- 백엔드: Cloud Run
- 데이터베이스: Cloud SQL MySQL

## 런타임 환경 변수

[cloudrun.env.example](/C:/Users/0122k/IdeaProjects/we-ai-server/cloudrun.env.example) 파일을 기본 템플릿으로 사용합니다.

운영 환경 핵심 규칙:

- Cloud Run에서는 `DB_HOST=localhost` 를 사용하지 않습니다.
- `DB_URL` 은 Cloud SQL JDBC Socket Factory 방식 사용을 우선합니다.
- `APP_BASE_URL` 과 `APP_LOCAL_BASE_URL` 은 Cloud Run의 전체 HTTPS URL이어야 합니다.
- `APP_API_DOMAIN` 은 `https://` 없이 Cloud Run 호스트명만 넣어야 합니다.
- 소셜 로그인 `*_REDIRECT_URI` 값은 각 제공자 콘솔에 등록된 값과 정확히 일치해야 합니다.
- 실제 이메일 인증을 사용하려면 아래 값이 필요합니다:
  - `AUTH_VERIFICATION_MOCK_ENABLED=false`
  - `AUTH_VERIFICATION_EXPOSE_CODE_IN_RESPONSE=false`
  - 유효한 `MAIL_*` 값

## 현재 운영 스냅샷

`2026-05-30` 기준으로 현재 운영 URL을 확인한 결과입니다:

- 백엔드 헬스:
  - `GET https://we-ai-server-167535321001.asia-northeast3.run.app/actuator/health`
  - `{"status":"UP", ...}` 반환
- Swagger:
  - `https://we-ai-server-167535321001.asia-northeast3.run.app/swagger-ui.html`
  - 접속 가능
- 프론트엔드:
  - `https://we-ai-client.vercel.app`
  - 접속 가능

중요:

- 현재 배포된 Cloud Run OpenAPI 문서에는 이 로컬 백엔드 워크트리에서 추가한 Project Settings의 새 멤버 상세 / 역할 / 부서 엔드포인트가 아직 노출되지 않습니다.
- 즉, 코드는 로컬에는 반영되어 있지만 운영 프론트엔드가 새 엔드포인트를 호출하려면 Cloud Run 새 리비전을 추가로 배포해야 합니다.

## 현재 Cloud Run 환경 변수 확인 방법

### 콘솔

1. Google Cloud Console
2. Cloud Run
3. `we-ai-server`
4. `Edit and deploy new revision`
5. `Container` 탭
6. `Variables & Secrets`

### Cloud Shell

```bash
gcloud run services describe we-ai-server --region asia-northeast3 --format export
```

아래 위치를 확인합니다:

```yaml
spec:
  template:
    spec:
      containers:
        - env:
```

## Cloud Run 환경 변수 수정 방법

### 콘솔

1. Cloud Run
2. `we-ai-server`
3. `Edit and deploy new revision`
4. `Container` 탭
5. `Variables & Secrets`
6. 값을 수정합니다
7. `Deploy`

### Cloud Shell

```bash
gcloud run services update we-ai-server \
  --region asia-northeast3 \
  --env-vars-file cloudrun.env
```

## 권장 운영 값

### 백엔드 URL 값

백엔드 URL이 아래와 같다면:

`https://we-ai-server-167535321001.asia-northeast3.run.app`

다음처럼 설정합니다:

```env
APP_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
APP_LOCAL_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
APP_API_DOMAIN=we-ai-server-167535321001.asia-northeast3.run.app
```

### 프론트엔드 값

프론트엔드가 Vercel의 `https://we-ai-client.vercel.app` 에 배포되어 있다면:

```env
APP_FRONTEND_BASE_URL=https://we-ai-client.vercel.app
APP_FRONTEND_DOMAIN=we-ai-client.vercel.app
```

### JWT issuer

실제 백엔드 origin 사용을 권장합니다:

```env
JWT_ISSUER=https://we-ai-server-167535321001.asia-northeast3.run.app
```

## 헬스 체크 문제 해결

`/actuator/health` 가 `DOWN` 을 반환하면 Spring health contributor 중 적어도 하나가 실패 중이라는 뜻입니다.

이 앱에서 가능성이 높은 원인:

- Cloud SQL 연결 문제
- SMTP 연결 문제

Cloud Shell 명령어:

```bash
gcloud run services logs read we-ai-server --region asia-northeast3 --limit 200
```

유용한 로그 키워드:

- `Communications link failure`
- `Access denied for user`
- `MailAuthenticationException`
- `Could not connect to SMTP host`
- `UnknownHostException`

참고:

- `/favicon.ico` 가 `401` 을 반환하는 것은 `health=DOWN` 의 원인이 아닙니다.
- Swagger가 정상적으로 열리는 것은 앱이 기동됐고 보안 설정상 Swagger 경로가 허용된다는 뜻일 뿐입니다.

## 브라우저에서 백엔드 검증 방법

### 헬스

아래 경로를 열어 확인합니다:

- `/actuator/health`
- `/swagger-ui.html`

### Swagger 스모크 테스트

1. Swagger UI를 엽니다
2. 아래와 같은 공개 인증 엔드포인트 하나를 펼칩니다:
   - `POST /api/v1/auth/signup`
   - `POST /api/v1/auth/login`
3. 테스트 payload로 실행합니다
4. 아래 항목을 확인합니다:
   - 응답 코드
   - 응답 본문
   - 500 에러 발생 여부

## Project Settings 작업용 Cloud Run 반영 체크리스트

새 백엔드 리비전을 배포한 뒤 아래 순서대로 확인합니다:

1. Cloud Run 리비전
   - Cloud Run 콘솔에 `we-ai-server` 의 새 최신 리비전이 보이는지 확인
   - 트래픽이 해당 리비전으로 라우팅되는지 확인
2. 헬스
   - `GET /actuator/health`
   - `status` 가 `UP` 인지 확인
3. Swagger UI
   - `/swagger-ui.html` 이 오류 없이 열리는지 확인
4. OpenAPI 경로 노출
   - `/v3/api-docs` 안에 아래 경로가 포함되어야 합니다:
     - `/api/v1/projects/{projectId}`
     - `/api/v1/projects/{projectId}/members/{memberId}`
     - `/api/v1/projects/{projectId}/members/{memberId}/role`
     - `/api/v1/projects/{projectId}/members/{memberId}/department`
5. 인증된 스모크 테스트
   - Swagger에서 로그인
   - 기존 프로젝트 엔드포인트 하나 호출
   - 실제 토큰으로 새 Project Settings 엔드포인트 4개 호출
6. 에러 처리
   - `PROJECT_NOT_FOUND` 검증
   - `PROJECT_ACCESS_DENIED` 검증
   - `PROJECT_LEADER_ONLY` 검증
   - `PROJECT_MEMBER_NOT_FOUND` 검증
7. 프론트엔드 스모크 테스트
   - `https://we-ai-client.vercel.app` 열기
   - Project Settings 화면으로 이동
   - 프로젝트 정보 수정과 팀 멤버 관련 동작이 새 Cloud Run 엔드포인트를 호출하는지 확인

배포 후 권장 `curl` 확인 명령:

```bash
curl -s https://we-ai-server-167535321001.asia-northeast3.run.app/actuator/health
curl -I -L https://we-ai-server-167535321001.asia-northeast3.run.app/swagger-ui.html
curl -s https://we-ai-server-167535321001.asia-northeast3.run.app/v3/api-docs | grep '/api/v1/projects/{projectId}/members/{memberId}'
```

## Vercel 프론트엔드 검증 방법

1. 배포된 Vercel 사이트를 Chrome에서 엽니다
2. `F12` 를 누릅니다
3. `Network` 탭을 엽니다
4. `Fetch/XHR` 를 클릭합니다
5. `Preserve log` 를 켭니다
6. 한 번 새로고침합니다
7. 회원가입/로그인 같은 실제 동작을 한 번 수행합니다
8. 요청 행을 클릭합니다

확인할 항목:

- `Request URL` 이 Cloud Run URL로 시작하는지
- status 가 `200` 또는 `201` 인지
- `401`, `403`, `500` 이면 `Response` 를 확인

## Vite 프록시를 사용하는 로컬 프론트엔드 검증 방법

프론트엔드 코드는 개발 모드에서 상대 경로 `/api/...` 를 사용합니다. [api.ts](/C:/Users/0122k/we-ai-client/src/app/lib/api.ts) 를 참고하세요.

1. 프론트엔드 프로젝트에서 아래 명령을 실행합니다:

```bash
npm run dev
```

2. 브라우저에서 로컬 프론트엔드를 엽니다
3. DevTools를 엽니다
4. `Network` 탭을 엽니다
5. 회원가입/로그인 동작을 수행합니다

확인할 항목:

- 브라우저 요청 URL이 `http://localhost:5173/api/...` 형태인지
- 응답 코드가 `200` 또는 `201` 인지
- 요청이 실패하면 `Response` 와 `Headers` 를 확인

브라우저가 `localhost:5173/api/...` 로 호출하고 있다면, Vite 프록시가 포워딩을 처리 중인 것입니다.

## 프론트엔드 환경 변수 연결 방식

현재 프론트엔드 동작은 아래 두 파일로 결정됩니다:

- [api.ts](/C:/Users/0122k/we-ai-client/src/app/lib/api.ts)
- [vite.config.ts](/C:/Users/0122k/we-ai-client/vite.config.ts)

실제 동작:

- 로컬 개발 브라우저 요청은 항상 상대 경로 `/api/...` 를 사용합니다
- Vite 프록시는 `/api` 를 아래 대상으로 전달합니다:
  - `VITE_API_BASE_URL` 이 설정되어 있으면 그 값
  - 없으면 `http://localhost:8080`
- 운영 빌드는 아래 규칙을 사용합니다:
  - `VITE_API_BASE_URL` 이 설정되어 있으면 `VITE_API_BASE_URL + /api/...`
  - `VITE_API_BASE_URL` 이 비어 있으면 상대 경로 `/api/...` 만 사용

즉:

- 로컬 프론트엔드는 Vite 실행 중에는 브라우저에서 Cloud Run을 직접 호출하지 않습니다
- 로컬 프론트엔드는 먼저 Vite로 요청하고, Vite가 로컬 백엔드 또는 Cloud Run으로 전달합니다
- 배포된 Vercel 프론트엔드는 `VITE_API_BASE_URL` 이 설정되어 있으면 브라우저에서 Cloud Run을 직접 호출합니다

### 권장 프론트엔드 환경 변수 값

#### Vercel Production

```env
VITE_API_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
```

#### Vercel Preview

```env
VITE_API_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
```

#### Cloud Run을 바라봐야 하는 로컬 프론트엔드

파일: `C:\Users\0122k\we-ai-client\.env`

```env
VITE_API_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
```

#### 로컬 백엔드를 바라봐야 하는 로컬 프론트엔드

`VITE_API_BASE_URL` 을 비워 두거나, 아래처럼 설정합니다:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 로컬 / 운영 대상 매트릭스

데이터가 어디에 기록되는지 제어하는 가장 쉬운 기준은 아래 표입니다:

| 프론트엔드 실행 환경 | `VITE_API_BASE_URL` | 실제 백엔드 대상 | 쓰기가 반영되는 DB |
| --- | --- | --- | --- |
| Vercel 운영 | `https://we-ai-server-167535321001.asia-northeast3.run.app` | Cloud Run | Cloud SQL MySQL |
| Vercel 프리뷰 | `https://we-ai-server-167535321001.asia-northeast3.run.app` | Cloud Run | Cloud SQL MySQL |
| 로컬 Vite 개발 | `https://we-ai-server-167535321001.asia-northeast3.run.app` | Vite 프록시를 통한 Cloud Run | Cloud SQL MySQL |
| 로컬 Vite 개발 | 미설정 또는 `http://localhost:8080` | Vite 프록시를 통한 로컬 Spring Boot | 로컬 Docker MySQL |

판단 규칙:

- 백엔드 대상이 Cloud Run이면 데이터는 Cloud SQL에 누적됩니다
- 백엔드 대상이 로컬 Spring Boot이면 데이터는 로컬 Docker MySQL에 누적됩니다

즉, 아래 조건을 유지하면 로컬 프론트엔드도 운영과 거의 동일하게 동작시킬 수 있습니다:

- 프론트엔드는 로컬 Vite 사용
- `VITE_API_BASE_URL` 은 Cloud Run을 가리키도록 설정
- 브라우저 요청은 `/api/...` 로 전송
- Vite 프록시가 Cloud Run으로 포워딩

## 프론트엔드에 영향이 있는 백엔드 CORS 값

현재 운영 백엔드 설정은 기본적으로 아래 브라우저 origin들을 허용합니다:

- `APP_FRONTEND_BASE_URL`
- `APP_VITE_FRONTEND_BASE_URL`
- `APP_ADDITIONAL_LOCAL_FRONTEND_BASE_URL`

현재 운영 YAML 기준 의도된 허용 origin은 아래와 같습니다:

- `https://we-ai-client.vercel.app`
- `http://localhost:5173`
- `http://127.0.0.1:5173`

이 설정이면 아래 경우는 충분히 커버됩니다:

- 운영 Vercel 도메인
- 로컬 Vite 개발

다만 아래 경우에는 부족할 수 있습니다:

- Vercel preview 배포 URL

preview 호출이 브라우저 CORS 에러로 실패하면, 해당 preview origin이 백엔드에 명시적으로 허용되어 있는지 확인해야 합니다.

## Vercel 환경 변수 확인

### Dashboard

1. Vercel Dashboard
2. `we-ai-client`
3. `Settings`
4. `Environment Variables`
5. 아래 대상에 `VITE_API_BASE_URL` 이 존재하는지 확인합니다:
   - `Production`
   - `Preview`

또한 값이 아래와 정확히 일치하는지 확인합니다:

```env
https://we-ai-server-167535321001.asia-northeast3.run.app
```

`VITE_API_BASE_URL` 이 없다면:

- 로컬 개발은 기본 프록시 대상인 `http://localhost:8080` 을 통해 여전히 동작할 수 있습니다
- Vercel 운영은 프론트엔드 origin 기준의 상대 경로 `/api/...` 를 호출하게 되어 Cloud Run에 도달하지 못합니다

### CLI

```bash
vercel env ls
vercel env ls production
vercel env ls preview
```

## Cloud Run CPU / 메모리 / 스케일링 확인

### 콘솔

1. Cloud Run
2. `we-ai-server`
3. `Edit and deploy new revision`
4. `Container` 탭

확인할 항목:

- CPU
- Memory
- Min instances
- Max instances
- Concurrency

### Cloud Shell

```bash
gcloud run services describe we-ai-server --region asia-northeast3 --format export
```

## 권장 시작 값

- CPU: `1`
- Memory: `1Gi`
- Min instances: `0`
- Max instances: `1`
- Concurrency: `10`

콜드 스타트가 너무 느리다면:

- `Min instances` 를 `1` 로 설정합니다

## OAuth 제공자 등록 체크리스트

### Kakao

- Kakao Login Redirect URI에 Cloud Run callback URI를 등록합니다
- Web platform 설정에 프론트엔드 웹 origin을 등록합니다

### Naver

- 서비스 URL을 등록합니다
- callback URL을 등록합니다

### Google

- Authorized JavaScript origins:
  - 프론트엔드 Vercel origin
- Authorized redirect URIs:
  - 백엔드 Cloud Run callback URL

## 보안 후속 조치

설정 과정에서 민감한 값이 노출되었으므로, 배포가 안정화된 뒤 아래 값을 교체하세요:

- DB 비밀번호
- Gmail 앱 비밀번호
- OAuth client secret
- JWT secret
