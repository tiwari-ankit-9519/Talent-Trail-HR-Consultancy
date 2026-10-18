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
- `DB_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `JWT_SECRET`, `FIELD_ENCRYPTION_KEY`: long random values
- `LOG_LEVEL_*`, `HTTP_BODIES_ENABLED`: as needed
- Business events: every non-security audit action (placements, invoices, payments, candidates, submissions, commissions, payroll) is also written as `EVENT <ACTION> <ENTITY>:<id> by <actor> - <summary>` to `logs/events.log` (`LOG_DIR`), kept 90 days, and to the normal log. Login and password events stay in the `audit` logger. Placement, invoice, candidate, submission and payroll run pages show a History card built from the audit data (`GET /api/v1/timeline/{PLACEMENT|INVOICE|CANDIDATE|SUBMISSION|COMMISSION|PAYROLL_RUN}/{id}`), limited to people who can view that record. Related records are merged, so a placement history includes its submission, interviews, invoices and commission.
- SQL logging: every executed query is written at INFO to the `sql` logger with its duration and bound values (`SQL 3 ms select ... | params=[...]`). Queries over `SLOW_QUERY_MS` log as `SLOW SQL` at WARN, failures at ERROR. Values for statements touching passwords, tokens, hashes, bank, PAN or UAN are hidden and long values are truncated. Set `SQL_LOG_PARAMS=false` to omit values or `SQL_LOG_ENABLED=false` to turn it off. Each access line also shows `queries=N dbMs=M`, and a request running `MANY_QUERIES_THRESHOLD` or more queries logs a `MANY QUERIES` warning (a likely N+1). Keep `LOG_LEVEL_SQL` and `LOG_LEVEL_SQL_PARAMS` at INFO, otherwise Hibernate prints each query a second time.

The backend refuses to start if a required value is missing or invalid.

## What is exposed

Only the proxy publishes ports (80 and 443). PostgreSQL, Redis, RabbitMQ and the backend are reachable only inside the Compose network. Only `/api/*` is forwarded to the backend, so actuator endpoints are not public.

## Monitoring and alerts

The backend checks itself every 5 minutes: database, RabbitMQ, Redis, stuck or failed outbox events, free disk space and memory. When a check starts failing, every Admin gets a notification in the bell and the addresses in `ALERT_EMAILS` get an email. It repeats every `MONITOR_REMINDER_HOURS` while the problem lasts and sends a "recovered" message when it clears. Admins can see the same checks live under Administration, System health.

A server that is completely down cannot report on itself, so run the optional Uptime Kuma service next to it:

```sh
docker compose --profile monitoring up -d uptime
```

It listens on `127.0.0.1:3001` only. Open it through an SSH tunnel (`ssh -L 3001:127.0.0.1:3001 user@server`), create the admin login, then add:

1. An HTTP(s) monitor for `https://YOUR_DOMAIN/api/v1/system/status` with keyword `UP`, every 60 seconds.
2. A Push monitor named Backup with a 25 hour heartbeat interval. Copy its push URL into `BACKUP_HEARTBEAT_URL` in `.env` and recreate the backup service. A missed or failed backup then raises an alert.
3. A notification channel (email, Telegram or Slack) and attach it to both monitors.

## Security notes

- Sign-in, token refresh and password change are limited per client IP (`AUTH_RATE_LIMIT_ATTEMPTS` per `AUTH_RATE_LIMIT_WINDOW_MINUTES`), on top of the per-account lockout.
- The backend takes the real client IP from the `X-Forwarded-For` header Caddy sets (only when the connection comes from a private network), so rate limits, audit rows and logs show the visitor and not the proxy.
- Caddy sends a Content-Security-Policy, Permissions-Policy and `no-store` on API responses, and returns 404 for `/actuator`.
- Backend and frontend containers run without extra Linux capabilities and cannot gain privileges.
- Access tokens last 15 minutes; deactivating a user or changing their password revokes refresh tokens immediately, and the access token expires within that window.

## Encryption key for bank and tax details

Bank account numbers, PAN and UAN are encrypted in the database with `FIELD_ENCRYPTION_KEY` (at least 32 random characters, different from `JWT_SECRET`). The backend will not start without it. Keep a copy in your password manager: if the key is lost or changed, the stored bank and tax details cannot be read and must be entered again. Backups contain the encrypted values only, so restoring a backup also needs the same key.

## Backups

The `backup` service writes a PostgreSQL dump and a tarball of uploaded files to the `backups` volume every day at 20:00 UTC (01:30 IST) and deletes copies older than `BACKUP_RETENTION_DAYS`.

Run one now:

```
docker compose run --rm backup --once
```

### Off-server copy (off by default)

Backups on the same disk do not protect against losing the server. Set `OFFSITE_ENABLED=true` in `.env` and recreate the backup service to also upload every new backup to remote storage:

```
docker compose up -d --build backup
```

Set `OFFSITE_ENABLED=false` (or remove it) and recreate the service to turn it off again. Nothing else changes and local backups keep running.

Settings:

| Variable                                                               | Meaning                                                                                                                           |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `OFFSITE_ENABLED`                                                      | `true` to upload, `false` to skip                                                                                                 |
| `OFFSITE_TYPE`                                                         | `s3` (default, works for AWS S3, Cloudflare R2, Backblaze B2, Wasabi, MinIO) or `sftp`                                            |
| `OFFSITE_PROVIDER`                                                     | S3 only: `Cloudflare`, `Backblaze`, `AWS`, `Wasabi` or `Other`                                                                    |
| `OFFSITE_ENDPOINT`, `OFFSITE_REGION`                                   | S3 only, from your storage provider (leave the endpoint empty for AWS)                                                            |
| `OFFSITE_ACCESS_KEY`, `OFFSITE_SECRET_KEY`                             | S3 only, an access key limited to one bucket                                                                                      |
| `OFFSITE_PATH`                                                         | Bucket and folder, for example `talent-trail-backups/prod` (for SFTP, the folder on the server)                                   |
| `OFFSITE_RETENTION_DAYS`                                               | Remote copies older than this are deleted (default 30)                                                                            |
| `OFFSITE_ENCRYPTION_PASSPHRASE`                                        | Optional. Encrypts file contents before upload. Keep this passphrase somewhere safe: without it the remote copies cannot be read. |
| `OFFSITE_SFTP_HOST`, `OFFSITE_SFTP_USER`, `OFFSITE_SFTP_PASS_OBSCURED` | SFTP only. Get the obscured password with `docker compose run --rm --entrypoint rclone backup obscure 'your-password'`            |

The backups contain employee and payroll data, so set an encryption passphrase before enabling off-server copies. If the upload fails, the backup run is reported as failed and the heartbeat is not sent, so Uptime Kuma alerts you.

### Restore

List local and off-server backups:

```
docker compose run --rm restore list
```

Download a backup set from off-server storage (the stamp is the `YYYYMMDD-HHMMSS` part of the file names):

```
docker compose run --rm restore pull 20261018-200000
```

Restore, with the app stopped first so nothing writes during the restore:

```
docker compose stop backend frontend
docker compose run --rm restore db db-20261018-200000.dump --yes
docker compose run --rm restore files files-20261018-200000.tar.gz --yes
docker compose start backend frontend
```

Do a trial restore into a test copy of the stack now and then. A backup you have never restored is not proven.

## Email templates and reminders

Admins edit email wording under Administration, Emails and reminders. Templates use placeholders such as `{{invoiceNumber}}`; only the placeholders listed for each template are accepted. The invoice email and the candidate offer email now use these templates.

Automatic reminders run daily at 9 AM in `APP_TIMEZONE` and are all off until enabled. Each reminder is sent once per listed day count; if the server was down, a missed day is caught up for up to two days. Set `REMINDERS_CRON` only to change the schedule (Spring cron format). Reminders need `MAIL_HOST` to be configured, otherwise nothing is sent.

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
