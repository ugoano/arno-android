package network.arno.android.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val title: String,
    val text: String?,
    @SerialName("package_name") val packageName: String,
    val timestamp: Long,
)

@Serializable
data class NotificationBridgeMessage(
    val type: String = "notification_bridge",
    val data: NotificationData,
    @SerialName("client_id") val clientId: String? = null,
)
