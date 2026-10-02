#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "    CONFIGURING ANDROID SDK FOR GITHUB CODESPACES"
echo "=========================================================="

# 1. Determine user home and SDK path (no sudo required!)
USER_HOME="${HOME:-/home/vscode}"
SDK_DIR="${ANDROID_HOME:-$USER_HOME/android-sdk}"

echo "Target Android SDK location: $SDK_DIR"
mkdir -p "$SDK_DIR"

# 2. Check and install prerequisite packages (curl, unzip, java) if in a Debian/Ubuntu container
if command -v apt-get >/dev/null 2>&1; then
    NEED_INSTALL=""
    command -v curl >/dev/null 2>&1 || NEED_INSTALL="$NEED_INSTALL curl"
    command -v unzip >/dev/null 2>&1 || NEED_INSTALL="$NEED_INSTALL unzip"
    command -v java >/dev/null 2>&1 || NEED_INSTALL="$NEED_INSTALL openjdk-17-jdk"

    if [ -n "$NEED_INSTALL" ]; then
        echo "Installing prerequisites:$NEED_INSTALL..."
        if command -v sudo >/dev/null 2>&1; then
            sudo apt-get update -qq && sudo apt-get install -y -qq $NEED_INSTALL || true
        else
            apt-get update -qq && apt-get install -y -qq $NEED_INSTALL || true
        fi
    fi
fi

# 3. Download Android Command-Line Tools if not already installed
CMDLINE_DIR="$SDK_DIR/cmdline-tools/latest"
if [ ! -f "$CMDLINE_DIR/bin/sdkmanager" ]; then
    echo "Downloading Android Command-Line Tools..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    TMP_ZIP="/tmp/cmdline-tools.zip"

    curl -sS -L -o "$TMP_ZIP" "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    unzip -q "$TMP_ZIP" -d "$SDK_DIR/cmdline-tools"
    rm -rf "$CMDLINE_DIR"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$CMDLINE_DIR"
    rm -f "$TMP_ZIP"
    echo "Android Command-Line Tools installed."
else
    echo "Android Command-Line Tools already present."
fi

# 4. Set up PATH and SDK environment variables
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$CMDLINE_DIR/bin:$SDK_DIR/platform-tools:$PATH"

# Persist environment variables to bashrc and profile
if [ -f "$USER_HOME/.bashrc" ]; then
    if ! grep -q "ANDROID_HOME" "$USER_HOME/.bashrc"; then
        echo "" >> "$USER_HOME/.bashrc"
        echo "# Android SDK" >> "$USER_HOME/.bashrc"
        echo "export ANDROID_HOME=\"$SDK_DIR\"" >> "$USER_HOME/.bashrc"
        echo "export ANDROID_SDK_ROOT=\"$SDK_DIR\"" >> "$USER_HOME/.bashrc"
        echo "export PATH=\"$CMDLINE_DIR/bin:\$ANDROID_HOME/platform-tools:\$PATH\"" >> "$USER_HOME/.bashrc"
    fi
fi

# 5. Accept licenses and install platform-tools, platform 36, build-tools 36.0.0
SDKMANAGER="$CMDLINE_DIR/bin/sdkmanager"
if [ -x "$SDKMANAGER" ]; then
    echo "Accepting Android SDK licenses..."
    yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1 || true

    echo "Installing Android SDK Platform 36 and Build-Tools..."
    "$SDKMANAGER" --sdk_root="$SDK_DIR" \
        "platform-tools" \
        "platforms;android-36" \
        "build-tools;36.0.0" || true
fi

# 6. Generate local.properties pointing to this SDK (only if outside AI Studio container)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -d "$PROJECT_DIR" ] && [ "$PROJECT_DIR" != "/app/applet" ]; then
    echo "sdk.dir=$SDK_DIR" > "$PROJECT_DIR/local.properties"
    echo "Created $PROJECT_DIR/local.properties pointing to $SDK_DIR"
fi

# 7. Make gradlew executable
if [ -f "$PROJECT_DIR/gradlew" ]; then
    chmod +x "$PROJECT_DIR/gradlew"
fi

# 8. Install Python websockets dependency for game server
if command -v pip3 >/dev/null 2>&1; then
    pip3 install --quiet websockets || true
elif command -v pip >/dev/null 2>&1; then
    pip install --quiet websockets || true
fi

echo "=========================================================="
echo "    ANDROID SDK SETUP COMPLETE!"
echo "=========================================================="
echo "Build command : ./gradlew assembleDebug"
echo "Server command: python server/server.py"
echo "=========================================================="
