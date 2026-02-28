#!/bin/bash
# deploy.sh - Deterministic Android APK deployment pipeline
# Usage: ./deploy.sh "Description of changes" [--skip-tests]
#
# Steps (all BLOCKING unless noted):
# 1. Git status check (BLOCKING)
# 2. Git push check (BLOCKING)
# 3. Version bump (versionCode +1, versionName patch bump)
# 4. Run unit tests (BLOCKING, skippable with --skip-tests)
# 5. Build APK
# 6. Copy APK to playground with cache-busted filename
# 7. Update download page (index.html) with version + description
# 8. Commit version bump + push
# 9. Print summary

set -euo pipefail

DESCRIPTION="${1:-"Bug fixes and improvements"}"
SKIP_TESTS=false
if [[ "${2:-}" == "--skip-tests" ]]; then
    SKIP_TESTS=true
fi

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_FILE="$REPO_DIR/app/build.gradle.kts"
PLAYGROUND_DIR="/Users/ugo/dev/arno-cloud/cc-web-bridge/playground/arno-android"
APK_SOURCE="$REPO_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_DEST="$PLAYGROUND_DIR/arno-debug.apk"
INDEX_FILE="$PLAYGROUND_DIR/index.html"
JAVA_HOME_PATH="/opt/homebrew/opt/openjdk@17"

echo "=== Arno Android Deploy Pipeline ==="
echo ""

# ── Step 1: Git status check (BLOCKING) ──
echo "[1/9] Checking git status..."
cd "$REPO_DIR"
DIRTY_FILES=$(git status --short)
if [[ -n "$DIRTY_FILES" ]]; then
    echo "BLOCKED: Uncommitted changes detected:"
    echo "$DIRTY_FILES"
    echo ""
    echo "Commit changes before deploying."
    exit 1
fi
echo "  Clean working tree"

# ── Step 2: Git push check (BLOCKING) ──
echo "[2/9] Checking push status..."
UNPUSHED=$(git log origin/main..HEAD --oneline 2>/dev/null || echo "")
if [[ -n "$UNPUSHED" ]]; then
    echo "BLOCKED: Unpushed commits:"
    echo "$UNPUSHED"
    echo ""
    echo "Push changes before deploying."
    exit 1
fi
echo "  All commits pushed"

# ── Step 3: Version bump ──
echo "[3/9] Bumping version..."
OLD_CODE=$(grep 'versionCode' "$GRADLE_FILE" | grep -o '[0-9]*')
OLD_NAME=$(grep 'versionName' "$GRADLE_FILE" | grep -o '"[^"]*"' | tr -d '"')

NEW_CODE=$((OLD_CODE + 1))
# Bump patch version: 1.30.0 -> 1.30.1
MAJOR=$(echo "$OLD_NAME" | cut -d. -f1)
MINOR=$(echo "$OLD_NAME" | cut -d. -f2)
PATCH=$(echo "$OLD_NAME" | cut -d. -f3)
NEW_PATCH=$((PATCH + 1))
NEW_NAME="${MAJOR}.${MINOR}.${NEW_PATCH}"

sed -i '' "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" "$GRADLE_FILE"
sed -i '' "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" "$GRADLE_FILE"
echo "  $OLD_NAME (build $OLD_CODE) -> $NEW_NAME (build $NEW_CODE)"

# ── Step 4: Run unit tests (BLOCKING unless --skip-tests) ──
if [[ "$SKIP_TESTS" == true ]]; then
    echo "[4/9] Skipping tests (--skip-tests)"
else
    echo "[4/9] Running unit tests..."
    if ! JAVA_HOME="$JAVA_HOME_PATH" ./gradlew testDebugUnitTest --quiet 2>&1; then
        echo "BLOCKED: Unit tests failed. Fix tests before deploying."
        # Revert version bump
        sed -i '' "s/versionCode = $NEW_CODE/versionCode = $OLD_CODE/" "$GRADLE_FILE"
        sed -i '' "s/versionName = \"$NEW_NAME\"/versionName = \"$OLD_NAME\"/" "$GRADLE_FILE"
        exit 1
    fi
    echo "  Tests passed"
fi

# ── Step 5: Build APK ──
echo "[5/9] Building APK..."
JAVA_HOME="$JAVA_HOME_PATH" ./gradlew assembleDebug --quiet 2>&1
APK_SIZE=$(ls -lh "$APK_SOURCE" | awk '{print $5}')
echo "  Built: $APK_SIZE"

# ── Step 6: Copy APK to playground ──
echo "[6/9] Copying APK to playground..."
cp "$APK_SOURCE" "$APK_DEST"
echo "  Copied to $APK_DEST"

# ── Step 7: Update download page with cache-busting ──
echo "[7/9] Updating download page..."
cat > "$INDEX_FILE" << HTMLEOF
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Arno Android</title>
<style>
body { background: #121212; color: #e0e0e0; font-family: system-ui, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }
.card { text-align: center; padding: 2rem; }
h1 { font-size: 1.5rem; margin-bottom: 0.5rem; }
p { color: #999; margin-bottom: 1.5rem; }
a { display: inline-block; background: #6750A4; color: white; padding: 1rem 2rem; border-radius: 12px; text-decoration: none; font-weight: 600; }
a:hover { background: #7C6DB5; }
.version { color: #666; font-size: 0.8rem; margin-top: 1rem; }
</style>
</head>
<body>
<div class="card">
<h1>Arno Android</h1>
<p>v${NEW_NAME} (build ${NEW_CODE}) - ${DESCRIPTION}</p>
<a href="arno-debug.apk?v=${NEW_CODE}" download="arno-debug.apk">Download APK</a>
<p class="version">${APK_SIZE}B - Debug build</p>
</div>
</body>
</html>
HTMLEOF
echo "  Updated index.html (cache-bust: ?v=$NEW_CODE)"

# ── Step 8: Commit version bump + push ──
echo "[8/9] Committing version bump..."
cd "$REPO_DIR"
git add app/build.gradle.kts
git commit -m "chore: bump version to $NEW_NAME (build $NEW_CODE)

$DESCRIPTION

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>" --quiet
git push --quiet
echo "  Committed and pushed"

# ── Step 9: Summary ──
echo ""
echo "=== Deploy Complete ==="
echo "  Version: $NEW_NAME (build $NEW_CODE)"
echo "  APK size: $APK_SIZE"
echo "  Description: $DESCRIPTION"
echo "  Download: https://chat.arno.network/playground/play/arno-android/"
echo ""
echo "Install the APK on your device to pick up the changes."
