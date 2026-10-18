#!/bin/sh
set -u

FILE="${1:-.env}"
errors=0
warnings=0

if [ ! -f "$FILE" ]; then
  echo "FAIL  $FILE not found"
  exit 1
fi

get() {
  line="$(grep -E "^$1=" "$FILE" | tail -n 1)"
  printf '%s' "${line#*=}"
}

fail() {
  echo "FAIL  $1"
  errors=$((errors + 1))
}

warn() {
  echo "WARN  $1"
  warnings=$((warnings + 1))
}

ok() {
  echo "PASS  $1"
}

require() {
  if [ -z "$(get "$1")" ]; then
    fail "$1 is empty"
    return 1
  fi
  return 0
}

secret_ok() {
  value="$(get "$1")"
  if [ -z "$value" ]; then
    fail "$1 is empty"
  elif [ "${#value}" -lt "$2" ]; then
    fail "$1 must be at least $2 characters"
  else
    case "$value" in
      change-me*|__*) fail "$1 still has the example value" ;;
      *) ok "$1" ;;
    esac
  fi
}

domain="$(get DOMAIN)"
case "$domain" in
  ""|http://*|localhost*|*localhost*|__*) fail "DOMAIN must be a real domain name without http:// (found '$domain')" ;;
  *) ok "DOMAIN $domain" ;;
esac

if [ "$(get APP_ENV)" = "production" ]; then ok "APP_ENV production"; else fail "APP_ENV must be production"; fi
if [ "$(get AUTH_COOKIE_SECURE)" = "true" ]; then ok "AUTH_COOKIE_SECURE true"; else fail "AUTH_COOKIE_SECURE must be true"; fi

origins="$(get CORS_ALLOWED_ORIGINS)"
if [ "$origins" = "https://$domain" ]; then ok "CORS_ALLOWED_ORIGINS"; else fail "CORS_ALLOWED_ORIGINS should be https://$domain (found '$origins')"; fi

secret_ok DB_PASSWORD 16
secret_ok REDIS_PASSWORD 16
secret_ok RABBITMQ_PASSWORD 16
secret_ok JWT_SECRET 32
secret_ok FIELD_ENCRYPTION_KEY 32

if [ -n "$(get JWT_SECRET)" ] && [ "$(get JWT_SECRET)" = "$(get FIELD_ENCRYPTION_KEY)" ]; then
  fail "JWT_SECRET and FIELD_ENCRYPTION_KEY must be different"
fi

for key in DB_URL DB_NAME DB_USER REDIS_HOST RABBITMQ_HOST RABBITMQ_USER MAIL_FROM; do
  require "$key" >/dev/null || true
done

bootstrap_password="$(get BOOTSTRAP_ADMIN_PASSWORD)"
if [ "$bootstrap_password" = "ChangeMe-First-Login-1" ]; then
  fail "BOOTSTRAP_ADMIN_PASSWORD still has the example value"
elif [ -n "$bootstrap_password" ]; then
  warn "BOOTSTRAP_ADMIN_PASSWORD is still set. Clear it after the first sign-in."
fi

if [ "$(get HTTP_BODIES_ENABLED)" = "true" ]; then fail "HTTP_BODIES_ENABLED must be false in production (bodies contain personal data)"; fi
if [ "$(get SQL_LOG_PARAMS)" != "false" ]; then warn "SQL_LOG_PARAMS is not false. Query values will be written to the logs."; fi
if [ "$(get LOG_LEVEL_APP)" = "DEBUG" ] || [ "$(get LOG_LEVEL_APP)" = "TRACE" ]; then warn "LOG_LEVEL_APP is $(get LOG_LEVEL_APP). Use INFO in production."; fi

if [ -z "$(get MAIL_HOST)" ]; then warn "MAIL_HOST is empty. Invoice, offer and reminder emails will not be sent."; fi
if [ "$(get OFFSITE_ENABLED)" != "true" ]; then warn "OFFSITE_ENABLED is not true. Backups stay on this server only."; fi
if [ -z "$(get BACKUP_HEARTBEAT_URL)" ]; then warn "BACKUP_HEARTBEAT_URL is empty. A failed backup will not raise an alert."; fi
if [ -z "$(get ALERT_EMAILS)" ]; then warn "ALERT_EMAILS is empty. Health alerts only reach the in-app bell."; fi

perm="$(stat -c '%a' "$FILE" 2>/dev/null || echo "")"
if [ -n "$perm" ] && [ "$perm" != "600" ] && [ "$perm" != "400" ]; then
  warn "$FILE permissions are $perm. Run: chmod 600 $FILE"
fi

echo
echo "$errors problem(s), $warnings warning(s)"
if [ "$errors" -gt 0 ]; then
  exit 1
fi
