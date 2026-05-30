# Cloud Run + Cloud SQL deployment guide

## Target architecture

- Frontend: Vercel
- Backend: Cloud Run
- Database: Cloud SQL MySQL

## Runtime environment variables

Use [cloudrun.env.example](/C:/Users/0122k/IdeaProjects/we-ai-server/cloudrun.env.example) as the base template.

Key production rules:

- Do not use `DB_HOST=localhost` on Cloud Run.
- Prefer `DB_URL` with the Cloud SQL JDBC Socket Factory.
- `APP_BASE_URL` and `APP_LOCAL_BASE_URL` should be the full Cloud Run HTTPS URL.
- `APP_API_DOMAIN` should be the Cloud Run host only, without `https://`.
- Social `*_REDIRECT_URI` values must exactly match what is registered in each provider console.
- Real email verification requires:
  - `AUTH_VERIFICATION_MOCK_ENABLED=false`
  - `AUTH_VERIFICATION_EXPOSE_CODE_IN_RESPONSE=false`
  - valid `MAIL_*` values

## Current live snapshot

Checked on `2026-05-30` against the current production URLs:

- Backend health:
  - `GET https://we-ai-server-167535321001.asia-northeast3.run.app/actuator/health`
  - returned `{"status":"UP", ...}`
- Swagger:
  - `https://we-ai-server-167535321001.asia-northeast3.run.app/swagger-ui.html`
  - reachable
- Frontend:
  - `https://we-ai-client.vercel.app`
  - reachable

Important:

- The currently deployed Cloud Run OpenAPI document does not yet expose the new Project Settings member-detail / role / department endpoints added in this local backend worktree.
- That means the code is implemented locally, but a new Cloud Run revision still needs to be deployed before the frontend can call those new endpoints in production.

## How to view current Cloud Run env vars

### Console

1. Google Cloud Console
2. Cloud Run
3. `we-ai-server`
4. `Edit and deploy new revision`
5. `Container` tab
6. `Variables & Secrets`

### Cloud Shell

```bash
gcloud run services describe we-ai-server --region asia-northeast3 --format export
```

Look under:

```yaml
spec:
  template:
    spec:
      containers:
        - env:
```

## How to update Cloud Run env vars

### Console

1. Cloud Run
2. `we-ai-server`
3. `Edit and deploy new revision`
4. `Container` tab
5. `Variables & Secrets`
6. edit values
7. `Deploy`

### Cloud Shell

```bash
gcloud run services update we-ai-server \
  --region asia-northeast3 \
  --env-vars-file cloudrun.env
```

## Recommended production values

### Backend URL values

If the backend URL is:

`https://we-ai-server-167535321001.asia-northeast3.run.app`

then:

```env
APP_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
APP_LOCAL_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
APP_API_DOMAIN=we-ai-server-167535321001.asia-northeast3.run.app
```

### Frontend values

If the frontend is deployed on Vercel at `https://we-ai-client.vercel.app`:

```env
APP_FRONTEND_BASE_URL=https://we-ai-client.vercel.app
APP_FRONTEND_DOMAIN=we-ai-client.vercel.app
```

### JWT issuer

Prefer using the real backend origin:

```env
JWT_ISSUER=https://we-ai-server-167535321001.asia-northeast3.run.app
```

## Health check troubleshooting

`/actuator/health` returning `DOWN` means at least one Spring health contributor is failing.

Most likely candidates in this app:

- Cloud SQL connectivity
- SMTP connectivity

Cloud Shell commands:

```bash
gcloud run services logs read we-ai-server --region asia-northeast3 --limit 200
```

Useful log keywords:

- `Communications link failure`
- `Access denied for user`
- `MailAuthenticationException`
- `Could not connect to SMTP host`
- `UnknownHostException`

Note:

- `/favicon.ico` returning `401` does not explain `health=DOWN`.
- Swagger opening successfully only proves the app started and that security permits Swagger paths.

## How to verify backend from browser

### Health

Open:

- `/actuator/health`
- `/swagger-ui.html`

### Swagger smoke tests

1. Open Swagger UI
2. Expand one public auth endpoint such as:
   - `POST /api/v1/auth/signup`
   - `POST /api/v1/auth/login`
3. Execute with test payload
4. Check:
   - response code
   - response body
   - any 500 errors

## Cloud Run reflection checklist for this Project Settings work

After deploying a new backend revision, confirm these in order:

1. Cloud Run revision
   - Cloud Run console shows a new latest revision for `we-ai-server`
   - traffic is routed to that revision
2. Health
   - `GET /actuator/health`
   - `status` is `UP`
3. Swagger UI
   - `/swagger-ui.html` opens without error
4. OpenAPI path exposure
   - `/v3/api-docs` contains:
     - `/api/v1/projects/{projectId}`
     - `/api/v1/projects/{projectId}/members/{memberId}`
     - `/api/v1/projects/{projectId}/members/{memberId}/role`
     - `/api/v1/projects/{projectId}/members/{memberId}/department`
5. Authenticated smoke test
   - login in Swagger
   - call one existing project endpoint
   - call the 4 new Project Settings endpoints with a real token
6. Error handling
   - verify `PROJECT_NOT_FOUND`
   - verify `PROJECT_ACCESS_DENIED`
   - verify `PROJECT_LEADER_ONLY`
   - verify `PROJECT_MEMBER_NOT_FOUND`
7. Frontend smoke test
   - open `https://we-ai-client.vercel.app`
   - navigate to Project Settings
   - confirm the project info edit and team member actions hit the new Cloud Run endpoints

Recommended `curl` checks after deploy:

```bash
curl -s https://we-ai-server-167535321001.asia-northeast3.run.app/actuator/health
curl -I -L https://we-ai-server-167535321001.asia-northeast3.run.app/swagger-ui.html
curl -s https://we-ai-server-167535321001.asia-northeast3.run.app/v3/api-docs | grep '/api/v1/projects/{projectId}/members/{memberId}'
```

## How to verify Vercel frontend

1. Open the deployed Vercel site in Chrome
2. Press `F12`
3. Open the `Network` tab
4. Click `Fetch/XHR`
5. Keep `Preserve log` enabled
6. Refresh once
7. Perform one real action such as signup/login
8. Click the request row

What to confirm:

- `Request URL` starts with the Cloud Run URL
- status is `200` or `201`
- if it is `401`, `403`, or `500`, inspect `Response`

## How to verify local frontend with Vite proxy

The frontend code uses relative `/api/...` paths in dev mode. See [api.ts](/C:/Users/0122k/we-ai-client/src/app/lib/api.ts).

1. In the frontend project:

```bash
npm run dev
```

2. Open the local frontend in the browser
3. Open DevTools
4. `Network` tab
5. Perform signup/login

What to confirm:

- the browser request URL looks like `http://localhost:5173/api/...`
- the response code is `200` or `201`
- if the request fails, inspect the `Response` and `Headers`

If the browser is calling `localhost:5173/api/...`, Vite proxy is handling the forwarding.

## Frontend environment variable wiring

The current frontend behavior is determined by these two files:

- [api.ts](/C:/Users/0122k/we-ai-client/src/app/lib/api.ts)
- [vite.config.ts](/C:/Users/0122k/we-ai-client/vite.config.ts)

Actual behavior:

- local dev browser requests always use relative `/api/...`
- Vite proxy forwards `/api` to:
  - `VITE_API_BASE_URL` if it is set
  - otherwise `http://localhost:8080`
- production build uses:
  - `VITE_API_BASE_URL + /api/...` when `VITE_API_BASE_URL` is set
  - relative `/api/...` only when `VITE_API_BASE_URL` is empty

In other words:

- local frontend does not call Cloud Run directly from the browser when running with Vite
- local frontend calls Vite first, and Vite forwards to either local backend or Cloud Run
- deployed Vercel frontend calls Cloud Run directly from the browser when `VITE_API_BASE_URL` is set

### Recommended frontend env values

#### Vercel Production

```env
VITE_API_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
```

#### Vercel Preview

```env
VITE_API_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
```

#### Local frontend that should hit Cloud Run

File: `C:\Users\0122k\we-ai-client\.env`

```env
VITE_API_BASE_URL=https://we-ai-server-167535321001.asia-northeast3.run.app
```

#### Local frontend that should hit local backend

Either unset `VITE_API_BASE_URL`, or use:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Local / production target matrix

This is the easiest way to control where data is written:

| Frontend runtime | `VITE_API_BASE_URL` | Actual backend target | DB that receives writes |
| --- | --- | --- | --- |
| Vercel production | `https://we-ai-server-167535321001.asia-northeast3.run.app` | Cloud Run | Cloud SQL MySQL |
| Vercel preview | `https://we-ai-server-167535321001.asia-northeast3.run.app` | Cloud Run | Cloud SQL MySQL |
| Local Vite dev | `https://we-ai-server-167535321001.asia-northeast3.run.app` | Cloud Run via Vite proxy | Cloud SQL MySQL |
| Local Vite dev | unset or `http://localhost:8080` | Local Spring Boot via Vite proxy | Local Docker MySQL |

Use this rule:

- if the backend target is Cloud Run, data accumulates in Cloud SQL
- if the backend target is local Spring Boot, data accumulates in local Docker MySQL

That means your local frontend can be made to behave almost exactly like production by keeping:

- frontend on local Vite
- `VITE_API_BASE_URL` pointing to Cloud Run
- browser requests going to `/api/...`
- Vite proxy forwarding to Cloud Run

## Backend CORS values that matter to the frontend

Current production backend configuration allows these browser origins by default:

- `APP_FRONTEND_BASE_URL`
- `APP_VITE_FRONTEND_BASE_URL`
- `APP_ADDITIONAL_LOCAL_FRONTEND_BASE_URL`

In the current production YAML, that means the intended allowed origins are:

- `https://we-ai-client.vercel.app`
- `http://localhost:5173`
- `http://127.0.0.1:5173`

This is enough for:

- the production Vercel domain
- local Vite development

But it may not be enough for:

- Vercel preview deployment URLs

If preview calls fail with a browser CORS error, check whether the preview origin is explicitly allowed by the backend.

## Vercel environment variable check

### Dashboard

1. Vercel Dashboard
2. `we-ai-client`
3. `Settings`
4. `Environment Variables`
5. Verify `VITE_API_BASE_URL` exists for:
   - `Production`
   - `Preview`

Also verify that the value is exactly:

```env
https://we-ai-server-167535321001.asia-northeast3.run.app
```

If `VITE_API_BASE_URL` is missing:

- local dev may still work through the default `http://localhost:8080` proxy target
- Vercel production will try to call relative `/api/...` on the frontend origin and will not reach Cloud Run

### CLI

```bash
vercel env ls
vercel env ls production
vercel env ls preview
```

## Cloud Run CPU / memory / scaling check

### Console

1. Cloud Run
2. `we-ai-server`
3. `Edit and deploy new revision`
4. `Container` tab

Check:

- CPU
- Memory
- Min instances
- Max instances
- Concurrency

### Cloud Shell

```bash
gcloud run services describe we-ai-server --region asia-northeast3 --format export
```

## Suggested starting values

- CPU: `1`
- Memory: `1Gi`
- Min instances: `0`
- Max instances: `1`
- Concurrency: `10`

If cold starts are too slow:

- set `Min instances` to `1`

## OAuth provider registration checklist

### Kakao

- Register the Cloud Run callback URI in Kakao Login Redirect URI
- Register the frontend web origin in the Web platform settings

### Naver

- Register the service URL
- Register the callback URL

### Google

- Authorized JavaScript origins:
  - frontend Vercel origin
- Authorized redirect URIs:
  - backend Cloud Run callback URL

## Security follow-up

Because sensitive values were exposed during setup, rotate these after deployment stabilizes:

- DB password
- Gmail app password
- OAuth client secrets
- JWT secret
