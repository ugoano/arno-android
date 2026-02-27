package network.arno.android.transport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Outgoing messages (client → bridge) ──

@Serializable
data class ClientRegistration(
    val type: String = "client_register",
    @SerialName("client_id") val clientId: String,
    @SerialName("client_type") val clientType: String = "android",
    val hostname: String,
    val capabilities: List<String>,
    @SerialName("priority_for") val priorityFor: Map<String, Int>,
)

@Serializable
data class HeartbeatMessage(
    val type: String = "heartbeat",
    @SerialName("client_id") val clientId: String,
    val timestamp: Double,
)

@Serializable
data class ChatMessage(
    val type: String = "message",
    val content: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("via_voice") val viaVoice: Boolean = false,
    @SerialName("image_ids") val imageIds: List<String> = emptyList(),
)

@Serializable
data class CancelMessage(
    val type: String = "cancel",
)

@Serializable
data class ReconnectMessage(
    val type: String = "reconnect",
    @SerialName("session_id") val sessionId: String,
    @SerialName("last_task_id") val lastTaskId: String? = null,
    @SerialName("last_chunk_offset") val lastChunkOffset: Int = 0,
)

@Serializable
data class CommandResponse(
    val type: String = "command_response",
    val id: String,
    val status: String,
    val result: JsonObject? = null,
    val error: String? = null,
)

// ── Incoming messages (bridge → client) ──

@Serializable
data class IncomingMessage(
    val type: String,
    val content: JsonElement? = null,
    val message: JsonElement? = null,
    val messages: kotlinx.serialization.json.JsonArray? = null,
    val delta: JsonObject? = null,
    val command: ClientCommand? = null,
    val session: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val tool: String? = null,
    val input: String? = null,
    val error: String? = null,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
    val summary: JsonObject? = null,
    val status: String? = null,
    val subtype: String? = null,
)

@Serializable
data class ClientCommand(
    val id: String,
    val type: String,
    val payload: JsonObject,
)
