#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA17_HOME="/Users/dongjulee/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home"

(
  cd "$ROOT_DIR/service-server"
  JAVA_HOME="$JAVA17_HOME" ./gradlew clean test bootJar
)

(
  cd "$ROOT_DIR/ai-server"
  uv sync
  uv run pytest
  uv run ruff check .
)

(
  cd "$ROOT_DIR/ai-training"
  uv sync
  uv run pytest
  uv run ruff check .
)

(
  cd "$ROOT_DIR/admin-web"
  npm ci
  npm run lint
  npm run test -- --run
  npm run build
)

(
  cd "$ROOT_DIR/app"
  fvm flutter pub get
  fvm flutter analyze
  fvm flutter test
)
