#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
REF="${1:-}"

if [ ! -f .env ]; then
  echo "No .env found. Run ops/init-env.sh first." >&2
  exit 1
fi

sh ops/check-env.sh .env

if [ -d .git ]; then
  previous="$(git rev-parse HEAD)"
  if [ -n "$REF" ]; then
    git fetch --tags --prune origin
    git checkout --quiet "$REF"
    if git symbolic-ref -q HEAD >/dev/null; then
      git pull --ff-only --quiet
    fi
  fi
fi

echo "Building images"
docker compose build

echo "Starting services"
docker compose up -d --remove-orphans

echo "Waiting for the application to become healthy"
tries=0
while docker compose ps --format '{{.Service}} {{.Health}}' | grep -E '^(backend|frontend) ' | grep -v ' healthy$' >/dev/null 2>&1; do
  tries=$((tries + 1))
  if [ "$tries" -gt 60 ]; then
    echo "Services did not become healthy in time" >&2
    docker compose ps >&2
    if [ -n "${previous:-}" ]; then
      echo "Roll back with: ./ops/rollback.sh" >&2
    fi
    exit 1
  fi
  sleep 5
done

domain="$(grep -E '^DOMAIN=' .env | tail -n 1 | cut -d= -f2-)"
if sh ops/smoke.sh "https://$domain"; then
  if [ -d .git ]; then
    git rev-parse HEAD > .last-good
  fi
  echo "Deploy finished"
else
  echo "Smoke test failed. Roll back with: ./ops/rollback.sh" >&2
  exit 1
fi
