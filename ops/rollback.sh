#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
TARGET="${1:-}"

if [ -z "$TARGET" ]; then
  if [ -f .last-good ]; then
    TARGET="$(cat .last-good)"
  else
    echo "No .last-good recorded. Pass a commit or tag: ./ops/rollback.sh <ref>" >&2
    exit 1
  fi
fi

echo "Rolling back to $TARGET"
git checkout --quiet "$TARGET"
docker compose build
docker compose up -d --remove-orphans
echo "Rolled back. Database changes are not reversed. If a migration ran, restore a backup (see docs/DEPLOYMENT.md)."
