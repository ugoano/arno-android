# Arno Android

Android client for [Arno](https://chat.arno.network) — a personal AI assistant powered by Claude. Connects to the CC Web Bridge via WebSocket as a first-class client alongside the [web](https://github.com/ugoano/arno-cloud) and [CLI](https://github.com/ugoano/arno-client) clients.

## Why Android?

The phone offers capabilities that browser tabs and terminal sessions cannot reliably provide:

- **Background speech** — Android TTS works with the screen locked via foreground service
- **Native notifications** — Rich system notifications with sound and vibration
- **Always-on connectivity** — Persistent WebSocket maintained by `START_STICKY` service
- **Priority routing** — The bridge automatically prefers Android for speech and notifications when connected

## Architecture

```
┌─────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)             │
│  ChatScreen, SettingsScreen             │
├─────────────────────────────────────────┤
│  ViewModel Layer                        │
│  ChatViewModel, SettingsViewModel       │
├─────────────────────────────────────────┤
│  Repository / Service Layer             │
│  ChatRepository, CommandExecutor        │
├─────────────────────────────────────────┤
│  Transport Layer                        │
│  ArnoWebSocket (OkHttp)                 │
│  ArnoService (Foreground Service)       │
└─────────────────────────────────────────┘
```

**Pattern:** MVVM with manual dependency injection via `AppContainer`. No Hilt — this is a personal tool, not a production app.

**Service ownership:** `ArnoService` (foreground service) owns the WebSocket connection and command executor. The `MainActivity` binds to the service to access shared state. This ensures the WebSocket stays alive when the app is backgrounded.

## Features

### Client Protocol
- WebSocket connection to `wss://chat.arno.network/ws`
- Client registration as `client_type: "android"` with 5 capabilities
- Heartbeat every 30s (matching bridge's `HEARTBEAT_INTERVAL`)
- Auto-reconnect with exponential backoff (1s → 2s → 4s → ... → 30s max)
- Deterministic `client_id` from `Build.MODEL` (prevents stale registry accumulation)
- `connection_lost` synthetic message on WebSocket failure (resets processing UI state)

### Command Execution
| Command | Android API | Priority |
|---------|------------|----------|
| `speak` | `TextToSpeech.speak()` | 1 (preferred) |
| `clipboard_copy` | `ClipboardManager.setPrimaryClip()` | 2 |
| `clipboard_paste` | `ClipboardManager.getPrimaryClip()` | 2 |
| `open_link` | `Intent(ACTION_VIEW)` | 2 |
| `notification` | `NotificationCompat.Builder` | 1 (preferred) |

### Voice Input
Three speech-to-text modes via Android `SpeechRecognizer`:

| Mode | Behaviour |
|------|-----------|
| **Push-to-talk** | Tap mic, speak, auto-send on silence |
| **Dictation** | Continuous listening, each phrase auto-sends |
| **Wake word** | Always listening for "Arno"/"Jarvis" — sends command portion |

Wake word supports both inline commands ("Arno, check my calendar") and bare activation ("Arno" → listens for follow-up → auto-sends).

### Audio Feedback
Custom WAV activation tones routed through `MediaPlayer` for Bluetooth audio compatibility:

| Tone | Method | When |
|------|--------|------|
| `LISTEN_START` | Custom WAV (`res/raw/bt_activation.wav`) | Voice activation (any mode) |
| `WAKE_WORD_DETECTED` | Custom WAV | Wake word recognised |
| `LISTEN_STOP` | ToneGenerator | Voice deactivated (guarded — only plays when actually listening) |
| `SPEECH_CAPTURED` | ToneGenerator | Speech captured before processing |

All tones route through `STREAM_MUSIC` for Bluetooth earphone/glasses audio output.

### Bluetooth Trigger
Media button voice trigger via `MediaSession` in foreground service:
- Tap Ray-Ban Meta temple or any BT media button to start voice input
- Works with the phone locked (foreground service)
- Plays custom WAV activation tone through BT audio
- Configurable via Settings toggle

### Chat Interface
- Streaming response rendering (accumulates `content_delta` messages)
- Markdown rendering via Markwon
- Tool call display with tool name badges
- File/image upload with multipart POST to bridge
- Non-image attachment rendering (paperclip icon + filename for audio, PDF, ZIP)
- Connection status banner with colour-coded indicator
- Connection resilience — `connection_lost` resets processing state on WebSocket failure
- Dark theme matching the web client aesthetic
- Auto-scroll to latest message

### Foreground Service
- `START_STICKY` — restarts if killed by system
- Persistent notification showing connection status
- TTS works with phone locked
- Commands handled even without app UI visible

## Project Structure

```
app/src/main/java/network/arno/android/
├── ArnoApp.kt                  # Application class
├── AppContainer.kt             # Manual DI container
├── MainActivity.kt             # Single-activity entry point
├── transport/
│   ├── ArnoWebSocket.kt        # OkHttp WebSocket with reconnection + connection_lost emission
│   ├── MessageTypes.kt         # Serializable protocol data classes
│   └── ConnectionState.kt      # Sealed class for connection states
├── service/
│   └── ArnoService.kt          # Foreground service (owns WebSocket, BT trigger via MediaSession)
├── command/
│   ├── CommandExecutor.kt      # Dictionary-based command dispatch
│   ├── SpeakHandler.kt         # Android TTS
│   ├── ClipboardHandler.kt     # System clipboard read/write
│   ├── LinkHandler.kt          # Intent-based URL opening
│   └── NotificationHandler.kt  # System notifications
├── voice/
│   ├── VoiceInputManager.kt    # 3-mode voice input (push-to-talk, dictation, wake word)
│   ├── VoiceMode.kt            # Enum: PUSH_TO_TALK, DICTATION, WAKE_WORD
│   ├── AudioFeedback.kt        # Custom WAV (MediaPlayer) + ToneGenerator dual-path audio
│   └── WakeWordDetector.kt     # "Arno"/"Jarvis" wake word extraction
├── chat/
│   ├── ChatMessage.kt          # Message data class
│   ├── ChatRepository.kt       # Message state + streaming + connection_lost handling
│   └── ChatViewModel.kt        # UI state management
├── settings/
│   ├── SettingsRepository.kt   # SharedPreferences persistence
│   └── SettingsViewModel.kt    # Settings UI state (voice mode, BT trigger)
└── ui/
    ├── theme/                  # Material3 dark theme
    ├── screens/                # ChatScreen, SettingsScreen, ArnoApp
    └── components/             # MessageBubble (image + file rendering), InputBar, ConnectionBanner

app/src/main/res/raw/
└── bt_activation.wav           # Custom WAV activation tone (trimmed, 0.88s)
```

## Dependencies

| Library | Purpose |
|---------|---------|
| OkHttp 4.12 | WebSocket client |
| kotlinx-serialization-json | Protocol message parsing |
| Jetpack Compose (BOM 2024.12) | Declarative UI |
| Material3 | Design system |
| Navigation Compose | Screen routing |
| Markwon 4.6 | Markdown rendering |
| Coil 2.x | Image loading (AsyncImage for attachment previews) |
| kotlinx-coroutines | Async operations |

## Building

### With build script (recommended)

The `build.sh` wrapper sets `JAVA_HOME` and `ANDROID_HOME` automatically:

```bash
./build.sh                    # assembleDebug (default)
./build.sh assembleRelease    # release build
./build.sh clean              # clean
./build.sh installDebug       # install to connected device
```

### With Android Studio

Open the project in Android Studio and build normally.

### With raw Gradle

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug
```

**Requirements:**
- JDK 17 (`brew install openjdk@17`)
- Android SDK 35, build-tools 34+35 (`brew install --cask android-commandlinetools`)
- Min SDK 29 (Android 10)

## Configuration

The app defaults to `https://chat.arno.network`. To change the server URL:

1. Open the app
2. Tap the gear icon (top right)
3. Edit the server URL
4. Tap "Save" — the app will reconnect on next launch

## Protocol Reference

This client implements the [Arno Client Protocol](https://github.com/ugoano/arno-cloud/blob/main/docs/ARNO_CLIENT_PROTOCOL.md). Key behaviours:

- **Registration:** Sends `client_register` immediately on WebSocket open
- **Heartbeat:** 30s interval via coroutine, prevents reaping after 120s
- **Routing:** Bridge prefers this client for `speak` and `notification` (priority 1)
- **Signals:** MCP-as-Signal architecture routes commands automatically — no manual routing needed
- **Responses:** Every command gets a `{type, id, status}` response back

## Verification

After installing, verify the client is registered:

```bash
curl https://chat.arno.network/api/clients | jq .
```

You should see an entry with `client_type: "android"` and all 5 capabilities listed.

## Related

- [arno-cloud](https://github.com/ugoano/arno-cloud) — CC Web Bridge (server)
- [arno-client](https://github.com/ugoano/arno-client) — CLI client (Python)
- [Arno Client Protocol](https://github.com/ugoano/arno-cloud/blob/main/docs/ARNO_CLIENT_PROTOCOL.md) — Protocol specification

## Arno Ops Skill Sync

When changes affect the Android client's capabilities, protocol behaviour, or command handling, update `arno-plugin/skills/arno-ops/SKILL.md` to keep Arno's self-knowledge accurate. Skip for cosmetic or internal-only changes.
