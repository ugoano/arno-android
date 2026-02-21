# Arno Android — Project Instructions

## Overview
Kotlin + Jetpack Compose Android app. Connects to CC Web Bridge (chat.arno.network) via WebSocket.

## Architecture
- MVVM with manual DI (`AppContainer`)
- `ArnoService` (foreground service) owns the WebSocket — survives app backgrounding
- `MainActivity` binds to service for UI access
- Dictionary-based command dispatch (mirrors `arno-client/dispatch.py`)
- Package: `network.arno.android`

## Key Patterns
- **Message types** use `kotlinx.serialization` — add `@Serializable` annotation
- **Command handlers** return `HandlerResult.Success()` or `HandlerResult.Error(msg)`
- **New commands** → add handler class in `command/`, register in `CommandExecutor.handlers` map
- **Streaming** → `ChatRepository.handleIncoming()` accumulates `content_delta` messages

## Protocol
- Client type: `android`
- Always include `client_id` in messages
- Heartbeat every 30s
- Auto-reconnect with exponential backoff (max 30s)
- Deterministic client ID from `Build.MODEL`

## Build
Use `./build.sh` — it sets JAVA_HOME and ANDROID_HOME for the Mac Mini environment.
```bash
./build.sh                    # assembleDebug (default)
./build.sh assembleRelease    # release build
./build.sh installDebug       # install to device
```

**Mac Mini paths (set by build.sh):**
- JAVA_HOME: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- ANDROID_HOME: `/opt/homebrew/share/android-commandlinetools`
