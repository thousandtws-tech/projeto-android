#!/usr/bin/env bash
# Pre-commit hook: runs the fast quality gates before every commit.
#
# Install (from the repo root):
#   cp scripts/pre-commit.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit
#
# Or point Git at a hooks directory instead (the file must be named
# "pre-commit", no extension):
#   git config core.hooksPath .githooks
#
# Bypass when needed: git commit --no-verify

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT_DIR"

echo "pre-commit: ./gradlew ktlintCheck detekt"
./gradlew ktlintCheck detekt
