#!/bin/sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
FILES_DIR="${FILES_DIR:-/data/files}"
RUN_HOUR="${BACKUP_HOUR_UTC:-20}"

. /ops/offsite.sh

run_backup() {
  stamp="$(date -u +%Y%m%d-%H%M%S)"
  mkdir -p "$BACKUP_DIR"
  PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -Fc \
    -f "$BACKUP_DIR/db-$stamp.dump.tmp"
  mv "$BACKUP_DIR/db-$stamp.dump.tmp" "$BACKUP_DIR/db-$stamp.dump"
  if [ -d "$FILES_DIR" ]; then
    tar -czf "$BACKUP_DIR/files-$stamp.tar.gz.tmp" -C "$FILES_DIR" .
    mv "$BACKUP_DIR/files-$stamp.tar.gz.tmp" "$BACKUP_DIR/files-$stamp.tar.gz"
  fi
  find "$BACKUP_DIR" -type f \( -name 'db-*.dump' -o -name 'files-*.tar.gz' \) -mtime +"$RETENTION_DAYS" -delete
  echo "backup $stamp complete"
  if offsite_enabled; then
    offsite_push "$stamp" || { echo "offsite copy failed" >&2; return 1; }
  fi
  if [ -n "${BACKUP_HEARTBEAT_URL:-}" ]; then
    wget -q -T 10 -O /dev/null "$BACKUP_HEARTBEAT_URL" || echo "heartbeat ping failed" >&2
  fi
}

seconds_until_run() {
  now_h="$(date -u +%H | sed 's/^0//')"
  now_m="$(date -u +%M | sed 's/^0//')"
  now_s="$(date -u +%S | sed 's/^0//')"
  now=$(( ${now_h:-0} * 3600 + ${now_m:-0} * 60 + ${now_s:-0} ))
  target=$(( RUN_HOUR * 3600 ))
  if [ "$target" -le "$now" ]; then
    target=$(( target + 86400 ))
  fi
  echo $(( target - now ))
}

if [ "${1:-}" = "--once" ]; then
  run_backup
  exit 0
fi

while true; do
  wait_for="$(seconds_until_run)"
  echo "next backup in ${wait_for}s"
  sleep "$wait_for"
  run_backup || echo "backup failed" >&2
done
