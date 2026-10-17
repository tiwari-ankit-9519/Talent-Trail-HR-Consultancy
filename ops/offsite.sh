#!/bin/sh
set -eu

offsite_enabled() {
  [ "${OFFSITE_ENABLED:-false}" = "true" ]
}

offsite_setup() {
  if [ -n "${OFFSITE_ENCRYPTION_PASSPHRASE:-}" ]; then
    RCLONE_CONFIG_OFFSITECRYPT_TYPE=crypt
    RCLONE_CONFIG_OFFSITECRYPT_REMOTE="offsite:${OFFSITE_PATH}"
    RCLONE_CONFIG_OFFSITECRYPT_FILENAME_ENCRYPTION=off
    RCLONE_CONFIG_OFFSITECRYPT_DIRECTORY_NAME_ENCRYPTION=false
    RCLONE_CONFIG_OFFSITECRYPT_PASSWORD="$(rclone obscure "$OFFSITE_ENCRYPTION_PASSPHRASE")"
    export RCLONE_CONFIG_OFFSITECRYPT_TYPE RCLONE_CONFIG_OFFSITECRYPT_REMOTE \
      RCLONE_CONFIG_OFFSITECRYPT_FILENAME_ENCRYPTION RCLONE_CONFIG_OFFSITECRYPT_DIRECTORY_NAME_ENCRYPTION \
      RCLONE_CONFIG_OFFSITECRYPT_PASSWORD
    OFFSITE_TARGET="offsitecrypt:"
  else
    OFFSITE_TARGET="offsite:${OFFSITE_PATH}"
  fi
}

offsite_check() {
  if [ -z "${OFFSITE_PATH:-}" ]; then
    echo "OFFSITE_PATH is required when OFFSITE_ENABLED=true" >&2
    return 1
  fi
  if [ -z "${RCLONE_CONFIG_OFFSITE_TYPE:-}" ]; then
    echo "OFFSITE_TYPE is required when OFFSITE_ENABLED=true" >&2
    return 1
  fi
}

offsite_push() {
  stamp="$1"
  offsite_check
  offsite_setup
  rclone copy "$BACKUP_DIR" "$OFFSITE_TARGET" --include "*-$stamp.*" --retries 3 --low-level-retries 5
  rclone delete "$OFFSITE_TARGET" --min-age "${OFFSITE_RETENTION_DAYS:-30}d" --retries 3
  echo "offsite copy for $stamp complete"
}

offsite_list() {
  offsite_check
  offsite_setup
  rclone lsl "$OFFSITE_TARGET"
}

offsite_pull() {
  stamp="$1"
  offsite_check
  mkdir -p "$BACKUP_DIR"
  offsite_setup
  rclone copy "$OFFSITE_TARGET" "$BACKUP_DIR" --include "*-$stamp.*" --retries 3
}
