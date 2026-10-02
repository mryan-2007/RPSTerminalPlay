#!/usr/bin/env bash
set -e

echo "=== Setting up Android SDK & Codespaces Environment ==="

# Make gradlew executable
chmod +x gradlew || true

# Set up Android SDK directory if not already set
SDK_DIR="${ANDROID_HOME:-/opt/android-sdk}"

if [ ! -d "$SDK_DIR/cmdline-tools" ]; then
    echo "Installing Android Command-Line Tools..."
    sudo mkdir -p "$SDK_DIR/cmdline-tools"
    sudo chown -R "$(whoami)" "$SDK_DIR"

    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    TMP_ZIP="/tmp/cmdline-tools.zip"

    curl -sS -o "$TMP_ZIP" "$CMDLINE_TOOLS_URL"
    unzip -q "$TMP_ZIP" -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -f "$TMP_ZIP"
fi

export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

if command -v sdkmanager >/dev/null 2>&1; then
    echo "Accepting Android SDK licenses..."
    yes | sdkmanager --licenses || true

    echo "Installing platform tools and Android 36 SDK..."
    sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" || true
fi

# Install Python websockets dependency for game server
if command -v pip3 >/dev/null 2>&1; then
    echo "Installing Python websockets..."
    pip3 install --quiet websockets || true
elif command -v pip >/dev/null 2>&1; then
    pip install --quiet websockets || true
fi

echo "=== Codespaces Android Environment Ready! ==="
echo "Run './gradlew assembleDebug' to build the APK."
echo "Run 'python server/server.py' to run the game server."
