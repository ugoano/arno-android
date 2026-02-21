package network.arno.android.sessions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    @SerialName("session_id") val sessionId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_modified") val lastModified: Double? = null,
    @SerialName("has_running_task") val hasRunningTask: Boolean = false,
    @SerialName("task_count") val taskCount: Int = 0,
    val preview: String? = null,
    val title: String? = null,
)

@Serializable
data class SessionsResponse(
    val sessions: List<Session>,
)

@Serializable
data class DeleteResponse(
    val deleted: Boolean? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val error: String? = null,
)

@Serializable
data class UpdateTitleResponse(
    @SerialName("session_id") val sessionId: String? = null,
    val title: String? = null,
    val error: String? = null,
)
