package network.arno.android.command

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.arno.android.R
import java.util.concurrent.atomic.AtomicInteger

class NotificationHandler(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "arno_commands"
        const val CHANNEL_NAME = "Arno Commands"
        private val notificationId = AtomicInteger(1000)
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications from Arno assistant"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun handle(payload: JsonObject): HandlerResult {
        val title = payload["title"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'title' in payload")
        val body = payload["body"]?.jsonPrimitive?.content ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_arno_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId.getAndIncrement(), notification)
        return HandlerResult.Success()
    }
}
