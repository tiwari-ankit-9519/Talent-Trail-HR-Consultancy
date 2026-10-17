# Talent Trail HR Solutions Platform

Internal operations platform for a recruitment consultancy. Phase 0 provides the platform foundation only.

## Stack

- Backend: Spring Boot 4.1.1, Java 25, Maven, Log4j 2, PostgreSQL, Flyway, Redis, RabbitMQ
- Frontend: Next.js (TypeScript)
- Edge: Caddy (HTTPS, reverse proxy)

## Run locally

```
cp .env.example .env
docker compose up --build
```

Open http://localhost. The home page shows whether the backend is reachable.

## Development and production differ only in `.env`

Every configurable value is read from `.env`. To deploy, copy the repository to the server, replace the content of `.env` with production values, and run `docker compose up -d --build`.

Values that must change for production:

- `DOMAIN`: your real domain, for example `hr.example.com` (Caddy then issues HTTPS certificates automatically)
- `CORS_ALLOWED_ORIGINS`: `https://hr.example.com`
- `DB_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `JWT_SECRET`: long random values
- `LOG_FORMAT`, `LOG_LEVEL_*`: as needed

The backend refuses to start if a required value is missing or invalid.

## What is exposed

Only the proxy publishes ports (80 and 443). PostgreSQL, Redis, RabbitMQ and the backend are reachable only inside the Compose network. Only `/api/*` is forwarded to the backend, so actuator endpoints are not public.

## Backups

The `backup` service writes a PostgreSQL dump and a tarball of uploaded files to the `backups` volume every day at 20:00 UTC (01:30 IST) and deletes copies older than `BACKUP_RETENTION_DAYS`.

Run one now:

```
docker compose run --rm backup --once
```

Copy the backups volume to storage outside the server regularly. Backups on the same disk do not protect against losing the server.

Restore a database dump:

```
docker compose exec -T postgres pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists < db-YYYYMMDD-HHMMSS.dump
```

## Layout

```
backend/    Spring Boot application (com.abchr.platform)
frontend/   Next.js application
ops/        Caddyfile and backup script
```

Backend packages are one per business module. `shared` holds configuration, the transactional outbox, request tracing and error handling.

## Phase 1: access control, clients and auditing

- First start creates the admin from `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`. The admin must change the password at first sign in. Clear both values afterwards.
- Roles hold permissions; Admin, Recruitment Manager, Recruiter, Account Manager, Finance Executive and HR Manager are seeded. More roles can be created on the Roles page.
- Every login, lockout, password change, user, role and client change (including commercial terms) and every denied request is written to the append-only audit log, visible on the Audit log page to holders of `AUDIT_VIEW`.
- Recruiters can see clients but not fee, GST or TDS terms (`CLIENTS_TERMS_VIEW`).
