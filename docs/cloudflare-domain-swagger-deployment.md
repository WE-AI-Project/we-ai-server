# Cloudflare Domain Deployment With Swagger Access

This document explains how to connect a Cloudflare-managed domain to this server, deploy the application, and open Swagger UI for API testing.

## Recommended topology

- Public API domain: `api.example.com`
- Optional staging API domain: `api-stag.example.com`
- Cloudflare proxy in front of nginx
- nginx forwards `/api/*`, `/swagger-ui.html`, `/swagger-ui/*`, `/v3/api-docs*`, and `/actuator/*` to the Spring Boot app

The nginx proxy routes are already configured in [docker/nginx/default.conf.template](C:/Users/0122k/IdeaProjects/we-ai-server/docker/nginx/default.conf.template).

## Important behavior in this repository

- `dev` keeps Swagger enabled by default.
- `stag` enables Swagger by default, but you can override it with `SWAGGER_ENABLED`.
- `prod` disables Swagger by default, and only enables it when `SWAGGER_ENABLED=true`.

Relevant files:

- [src/main/resources/application.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application.yml)
- [src/main/resources/application-stag.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-stag.yml)
- [src/main/resources/application-prod.yml](C:/Users/0122k/IdeaProjects/we-ai-server/src/main/resources/application-prod.yml)
- [compose.yaml](C:/Users/0122k/IdeaProjects/we-ai-server/compose.yaml)

## 1. Prepare a domain in Cloudflare

Create a DNS record for one of these:

- `api.example.com` for production
- `api-stag.example.com` for staging

Recommended:

- Use a dedicated subdomain for the backend instead of the root domain.
- Keep Swagger open on staging first, then open it on production only when necessary.

## 2. Choose the origin connection method

### Option A. Recommended: Cloudflare Tunnel

Use Cloudflare Tunnel to expose the server without opening public inbound ports directly.

Point the tunnel public hostname to:

- `http://localhost:80` if nginx is published on the host

This repository already publishes nginx on host port `80` by default through Docker Compose.

### Option B. DNS record to server public IP

If you point the Cloudflare DNS record directly to the server IP, you must also handle:

- inbound firewall rules
- HTTPS between Cloudflare and the origin
- SSL mode and certificate setup on the origin

If you want the simplest setup, use Tunnel.

## 3. Prepare the environment file

Use one of the example files:

- `.env.stag.example`
- `.env.prod.example`

Typical staging values:

```env
APP_PROFILE=stag
NGINX_SERVER_NAME=api-stag.example.com
APP_BASE_URL=https://api-stag.example.com
APP_API_DOMAIN=api-stag.example.com
APP_FRONTEND_BASE_URL=https://stag.example.com
APP_FRONTEND_DOMAIN=stag.example.com
SWAGGER_ENABLED=true
KAKAO_REDIRECT_URI=https://api-stag.example.com/api/v1/auth/kakao/callback
NAVER_REDIRECT_URI=https://api-stag.example.com/api/v1/auth/naver/callback
GOOGLE_REDIRECT_URI=https://api-stag.example.com/api/v1/auth/google/callback
```

Typical production values:

```env
APP_PROFILE=prod
NGINX_SERVER_NAME=api.example.com
APP_BASE_URL=https://api.example.com
APP_API_DOMAIN=api.example.com
APP_FRONTEND_BASE_URL=https://app.example.com
APP_FRONTEND_DOMAIN=app.example.com
SWAGGER_ENABLED=false
KAKAO_REDIRECT_URI=https://api.example.com/api/v1/auth/kakao/callback
NAVER_REDIRECT_URI=https://api.example.com/api/v1/auth/naver/callback
GOOGLE_REDIRECT_URI=https://api.example.com/api/v1/auth/google/callback
```

Notes:

- Update the OAuth redirect URIs in each provider console as well.
- `APP_BASE_URL` should match the public API domain used by Swagger and OpenAPI metadata.

## 4. Deploy with Docker Compose

For staging:

```bash
docker compose --env-file .env.stag --profile stag up -d --build
```

For production:

```bash
docker compose --env-file .env.prod --profile prod up -d --build
```

## 5. Verify the deployment

Check health first:

- `https://api-stag.example.com/actuator/health`
- `https://api.example.com/actuator/health`

Open Swagger UI next:

- `https://api-stag.example.com/swagger-ui.html`
- `https://api.example.com/swagger-ui.html`

OpenAPI JSON:

- `https://api-stag.example.com/v3/api-docs`
- `https://api.example.com/v3/api-docs`

## 6. Safer production usage

Recommended approach:

- Keep production Swagger off by default.
- Use staging for routine API testing.
- Temporarily set `SWAGGER_ENABLED=true` in production only when needed.
- Protect Swagger and OpenAPI endpoints with Cloudflare Access if they must be reachable externally.

Suggested Cloudflare Access protection targets:

- `/swagger-ui.html`
- `/swagger-ui/*`
- `/v3/api-docs*`

## 7. Troubleshooting

If `swagger-ui.html` does not open:

- Confirm `SWAGGER_ENABLED=true` for the active profile.
- Confirm the `app` container received `SWAGGER_ENABLED`.
- Confirm nginx is up and published on host port `80`.
- Confirm the Cloudflare hostname points to the correct origin.
- Confirm `/actuator/health` is reachable first.

If OAuth login breaks after domain change:

- Check `KAKAO_REDIRECT_URI`
- Check `NAVER_REDIRECT_URI`
- Check `GOOGLE_REDIRECT_URI`
- Check the same redirect URIs in each provider console
