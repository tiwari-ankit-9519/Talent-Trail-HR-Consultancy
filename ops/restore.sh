#!/bin/sh
set -eu

. /ops/offsite.sh

BACKUP_DIR="${BACKUP_DIR:-/backups}"
FILES_DIR="${FILES_DIR:-/data/files}"

usage() {
  cat <<'U'
usage:
  restore.sh list                      show local backups and, if enabled, off-server backups
  restore.sh pull <stamp>              download one backup set from off-server storage
  restore.sh db <file> --yes           replace the database with a dump from the backups folder
  restore.sh files <file> --yes        replace uploaded files with a tarball from the backups folder
U
}

confirm() {
  if [ "${1:-}" != "--yes" ]; then
    echo "this replaces live data; repeat the command with --yes to continue" >&2
    exit 2
  fi
}

case "${1:-}" in
  list)
    echo "local:"
    ls -1 "$BACKUP_DIR" 2>/dev/null || true
    if offsite_enabled; then
      echo "off-server:"
      offsite_list
    fi
    ;;
  pull)
    offsite_enabled || { echo "off-server backups are not enabled" >&2; exit 1; }
    [ -n "${2:-}" ] || { usage; exit 1; }
    offsite_pull "$2"
    ls -1 "$BACKUP_DIR"
    ;;
  db)
    [ -n "${2:-}" ] || { usage; exit 1; }
    confirm "${3:-}"
    PGPASSWORD="$DB_PASSWORD" pg_restore -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" --clean --if-exists \
      --no-owner "$BACKUP_DIR/$2"
    echo "database restored from $2"
    ;;
  files)
    [ -n "${2:-}" ] || { usage; exit 1; }
    confirm "${3:-}"
    mkdir -p "$FILES_DIR"
    tar -xzf "$BACKUP_DIR/$2" -C "$FILES_DIR"
    echo "files restored from $2"
    ;;
  *)
    usage
    exit 1
    ;;
esac
