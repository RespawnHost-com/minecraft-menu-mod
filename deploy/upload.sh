#!/usr/bin/env bash
set -euo pipefail
args=()
if [[ "${DRY_RUN:-0}" == 1 ]]; then args+=(--dry-run); fi
exec python3 "$(dirname "$0")/upload.py" "${1:-artifacts}" "${args[@]}"
