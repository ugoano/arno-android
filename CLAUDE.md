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
```bash
./gradlew assembleDebug
./gradlew installDebug
```
