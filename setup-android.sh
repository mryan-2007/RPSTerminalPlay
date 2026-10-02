#!/usr/bin/env bash
# Root helper to configure Android SDK on GitHub Codespaces or any Linux machine
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
bash "$SCRIPT_DIR/.devcontainer/setup-android-sdk.sh" "$@"
