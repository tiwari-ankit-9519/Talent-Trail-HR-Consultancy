#!/bin/sh
set -eu

if [ "$#" -lt 2 ]; then
  echo "Usage: ops/init-env.sh <domain> <admin-email> [output-file]" >&2
  echo "Example: ops/init-env.sh hr.example.com admin@example.com" >&2
  exit 1
fi

DOMAIN="$1"
ADMIN_EMAIL="$2"
OUT="${3:-.env}"
TEMPLATE="$(dirname "$0")/../.env.production.example"

if [ -e "$OUT" ]; then
  echo "$OUT already exists. Move it away first so secrets are not overwritten." >&2
  exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required" >&2
  exit 1
fi

secret() {
  openssl rand -base64 64 | tr -dc 'A-Za-z0-9' | cut -c1-"$1"
}

ADMIN_PASSWORD="Aa1-$(secret 16)"

umask 077
sed \
  -e "s|__DOMAIN__|$DOMAIN|g" \
  -e "s|__ADMIN_EMAIL__|$ADMIN_EMAIL|g" \
  -e "s|__ADMIN_PASSWORD__|$ADMIN_PASSWORD|g" \
  -e "s|__DB_PASSWORD__|$(secret 32)|g" \
  -e "s|__REDIS_PASSWORD__|$(secret 32)|g" \
  -e "s|__RABBITMQ_PASSWORD__|$(secret 32)|g" \
  -e "s|__JWT_SECRET__|$(secret 64)|g" \
  -e "s|__FIELD_ENCRYPTION_KEY__|$(secret 64)|g" \
  "$TEMPLATE" > "$OUT"

echo "Created $OUT with new random secrets."
echo
echo "First sign-in:  $ADMIN_EMAIL"
echo "Password:       $ADMIN_PASSWORD"
echo "You must change this password at first sign-in. Then clear BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD in $OUT."
echo
echo "Copy FIELD_ENCRYPTION_KEY and the whole file to your password manager now."
echo "Without FIELD_ENCRYPTION_KEY, stored bank and tax details cannot be read after a restore."
