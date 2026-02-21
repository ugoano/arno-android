#!/usr/bin/env bash
#
# Arno Android — build helper
# Sets JAVA_HOME and ANDROID_HOME, then forwards all arguments to gradlew.
#
# Usage:
#   ./build.sh                    # assembleDebug (default)
#   ./build.sh assembleRelease    # release build
#   ./build.sh clean              # clean
#   ./build.sh test               # run tests

set -euo pipefail

# ── Environment ──────────────────────────────────────────────────────
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"

# Verify JDK
if [ ! -d "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME not found at $JAVA_HOME"
    echo "Install with: brew install openjdk@17"
    exit 1
fi

# Verify Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME not found at $ANDROID_HOME"
    echo "Install with: brew install --cask android-commandlinetools"
    exit 1
fi

# ── Build ────────────────────────────────────────────────────────────
TASK="${1:-assembleDebug}"

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"
echo "Running: ./gradlew $*"
echo "──────────────────────────────────────"

cd "$(dirname "$0")"
./gradlew "$@"
