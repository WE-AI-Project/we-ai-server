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

## Vercel environment variable check

### Dashboard

1. Vercel Dashboard
2. `we-ai-client`
3. `Settings`
4. `Environment Variables`
5. Verify `VITE_API_BASE_URL` exists for:
   - `Production`
   - `Preview`

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
