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
- **Connection resilience** → `ArnoWebSocket.onFailure` emits synthetic `connection_lost` message → `ChatRepository` resets `isProcessing` and finalises streaming

## Voice System
- **VoiceInputManager** — 3 modes (PUSH_TO_TALK, DICTATION, WAKE_WORD) via Android `SpeechRecognizer`
- **AudioFeedback** — dual-path audio: custom WAV via `MediaPlayer` for activation tones (`LISTEN_START`, `WAKE_WORD_DETECTED`), `ToneGenerator` for feedback tones (`LISTEN_STOP`, `SPEECH_CAPTURED`)
  - `ACTIVATION_TONES` set determines routing: WAV when context + resource available, ToneGenerator fallback
  - Constructor requires `Context` for custom WAV: `AudioFeedback(context)`
  - Custom WAV: `res/raw/bt_activation.wav` (trimmed, 0.88s)
  - All tones use `STREAM_MUSIC` for Bluetooth routing
- **WakeWordDetector** — detects "Arno"/"Jarvis" in transcript, extracts command portion
- **BT Trigger** — `ArnoService` owns `MediaSession` for media button events; `btAudioFeedback` is `lateinit` (needs `applicationContext`)
- **Stop tone guard** — `VoiceInputManager.stop()` only plays `LISTEN_STOP` when `_isListening.value || _isContinuousActive.value` (prevents tone on tab switch)

## File Attachments
- **Image URIs** — rendered via Coil `AsyncImage` (jpg, png, gif, webp, bmp)
- **Non-image URIs** — rendered via `MessageFileList` composable (paperclip icon + filename)
- **Upload** — multipart POST to bridge; bridge supports audio MIME types (wav, mpeg, ogg, mp4)

## Protocol
- Client type: `android`
- Always include `client_id` in messages
- Heartbeat every 30s
- Auto-reconnect with exponential backoff (max 30s)
- Deterministic client ID from `Build.MODEL`
- `connection_lost` synthetic message type (internal, emitted by `ArnoWebSocket.onFailure`)

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
