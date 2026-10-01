#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
docker compose up -d postgres
docker compose exec -T postgres pg_isready -U "${POSTGRES_USER:-smart_parking}" -d "${POSTGRES_DB:-smart_parking}"
docker compose exec -T postgres psql -U "${POSTGRES_USER:-smart_parking}" -d "${POSTGRES_DB:-smart_parking}" -c "CREATE EXTENSION IF NOT EXISTS postgis; SELECT postgis_full_version();"
docker compose stop
