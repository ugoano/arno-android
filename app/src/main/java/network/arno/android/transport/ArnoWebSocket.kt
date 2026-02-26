package network.arno.android.transport

import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ArnoWebSocket(
    private val serverUrl: String,
    private val onCommand: (ClientCommand) -> CommandResponse,
    private val reconnectionReadyGate: ReconnectionReadyGate = ReconnectionReadyGate(),
) {
    companion object {
        private const val TAG = "ArnoWebSocket"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val BACKOFF_MAX_S = 30.0
        private const val NORMAL_CLOSE_CODE = 1000

        val CAPABILITIES = listOf(
            "speak", "clipboard_copy", "clipboard_paste", "open_link", "notification",
            "close_tab", "device_control"
        )

        val PRIORITIES = mapOf(
            "speak" to 1,
            "notification" to 1,
            "clipboard_copy" to 2,
            "clipboard_paste" to 2,
            "open_link" to 2,
            "close_tab" to 2,
            "device_control" to 2,
        )
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val clientId: String = generateClientId()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages
    val reconnectInProgress: StateFlow<Boolean> = reconnectionReadyGate.reconnectInProgress

    private var sessionId: String? = null
    var currentSessionId: String?
        get() = sessionId
        private set(value) { sessionId = value }

    // When set, the next connect() will send a reconnect message for this session
    private var pendingReconnectSessionId: String? = null

    private fun generateClientId(): String {
        val model = Build.MODEL.lowercase().replace(" ", "_")
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(model.toByteArray())
            .take(4)
            .joinToString("") { "%02x".format(it) }
        return "android_$hash"
    }

    fun connect() {
        if (_connectionState.value is ConnectionState.Connected ||
            _connectionState.value is ConnectionState.Connecting) return

        _connectionState.value = ConnectionState.Connecting
        val wsUrl = serverUrl.replace("https://", "wss://").replace("http://", "ws://")
            .trimEnd('/') + "/ws"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, createListener())
    }

    /**
     * Connect (or reconnect) targeting a specific session.
     * After the WebSocket opens, sends a reconnect message so the bridge
     * restores that session's context and replays chat history.
     */
    fun connectToSession(targetSessionId: String) {
        pendingReconnectSessionId = targetSessionId
        currentSessionId = targetSessionId
        reconnectionReadyGate.onReconnectStarted()
        connect()
    }

    fun disconnect() {
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        webSocket?.close(NORMAL_CLOSE_CODE, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendMessage(text: String, viaVoice: Boolean = false, imageIds: List<String> = emptyList()) {
        val msg = ChatMessage(
            content = text,
            clientId = clientId,
            viaVoice = viaVoice,
            imageIds = imageIds,
        )
        send(json.encodeToString(msg))
    }

    fun sendCancel() {
        val msg = CancelMessage()
        send(json.encodeToString(msg))
        Log.i(TAG, "Cancel request sent")
    }

    private fun send(text: String) {
        webSocket?.send(text) ?: Log.w(TAG, "WebSocket not connected, cannot send")
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket connected")
            reconnectAttempt = 0
            _connectionState.value = ConnectionState.Connected
            sendRegistration()

            // If switching to a specific session, tell the bridge
            pendingReconnectSessionId?.let { targetSession ->
                pendingReconnectSessionId = null
                val reconnect = ReconnectMessage(sessionId = targetSession)
                send(json.encodeToString(reconnect))
                Log.i(TAG, "Sent reconnect for session: ${targetSession.take(20)}...")
            }

            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closing: $code $reason")
            webSocket.close(NORMAL_CLOSE_CODE, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closed: $code $reason")
            heartbeatJob?.cancel()
            if (code != NORMAL_CLOSE_CODE) {
                scheduleReconnect()
            } else {
                _connectionState.value = ConnectionState.Disconnected
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            heartbeatJob?.cancel()
            _connectionState.value = ConnectionState.Error(t.message ?: "Unknown error")
            // Reset processing state so UI doesn't show stale spinner
            scope.launch {
                _incomingMessages.emit(IncomingMessage(type = "connection_lost"))
            }
            scheduleReconnect()
        }
    }

    private fun sendRegistration() {
        val reg = ClientRegistration(
            clientId = clientId,
            hostname = Build.MODEL,
            capabilities = CAPABILITIES,
            priorityFor = PRIORITIES,
        )
        send(json.encodeToString(reg))
        Log.i(TAG, "Registered as $clientId (${Build.MODEL})")
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                val hb = HeartbeatMessage(
                    clientId = clientId,
                    timestamp = System.currentTimeMillis() / 1000.0,
                )
                send(json.encodeToString(hb))
                Log.d(TAG, "Heartbeat sent")
            }
        }
    }

    private fun handleMessage(raw: String) {
        try {
            val msg = json.decodeFromString<IncomingMessage>(raw)

            when (msg.type) {
                "client_command" -> {
                    val cmd = msg.command ?: return
                    Log.d(TAG, "Received command: ${cmd.type}")
                    val response = onCommand(cmd)
                    send(json.encodeToString(response))
                    Log.d(TAG, "Command response sent: ${response.status}")
                }
                "session" -> {
                    currentSessionId = msg.sessionId
                    Log.i(TAG, "Session assigned: ${msg.sessionId}")
                }
                "chat_history" -> {
                    reconnectionReadyGate.onChatHistoryReceived()
                    // Forward to ChatRepository for rendering
                    Log.i(TAG, "Received chat_history for session: ${msg.sessionId?.take(20)}")
                    scope.launch { _incomingMessages.emit(msg) }
                }
                "heartbeat_ack" -> {
                    Log.d(TAG, "Heartbeat acknowledged")
                }
                "client_registered" -> {
                    Log.i(TAG, "Client registration acknowledged")
                }
                else -> {
                    // Forward to UI layer via shared flow
                    // Includes: assistant, content_block_delta, result, user, system, error, etc.
                    scope.launch { _incomingMessages.emit(msg) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse message: ${e.message}\nRaw: ${raw.take(300)}")
        }
    }

    private var reconnectAttempt = 0

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            currentSessionId?.let { sessionId ->
                pendingReconnectSessionId = sessionId
                reconnectionReadyGate.onReconnectStarted()
            }
            val delayS = calculateBackoff(reconnectAttempt)
            _connectionState.value = ConnectionState.Reconnecting(reconnectAttempt + 1)
            Log.i(TAG, "Reconnecting in ${delayS}s (attempt ${reconnectAttempt + 1})")
            delay((delayS * 1000).toLong())
            reconnectAttempt++
            connect()
        }
    }

    private fun calculateBackoff(attempt: Int): Double {
        val delay = Math.pow(2.0, attempt.toDouble())
        return delay.coerceAtMost(BACKOFF_MAX_S)
    }

    fun resetReconnectCounter() {
        reconnectAttempt = 0
    }

    fun destroy() {
        scope.cancel()
        disconnect()
        client.dispatcher.executorService.shutdown()
    }
}
