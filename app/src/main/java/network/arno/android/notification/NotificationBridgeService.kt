package network.arno.android.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import network.arno.android.ArnoApp

/**
 * System-managed service that captures device notifications and forwards
 * them to the CC Web Bridge via the existing WebSocket connection.
 *
 * Requires Notification Access permission granted by the user in
 * Android Settings > Apps > Special app access > Notification access.
 */
class NotificationBridgeService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifBridgeService"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val container = try {
            (application as ArnoApp).container
        } catch (e: Exception) {
            Log.w(TAG, "AppContainer not available: ${e.message}")
            return
        }

        // Check if feature is enabled
        if (!container.settingsRepository.notificationBridgeEnabled) return

        // Build filter from settings
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = container.settingsRepository.notificationWhitelist,
            ownPackage = packageName,
        )

        if (!filter.shouldCapture(sbn.packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()

        val data = NotificationData(
            title = title,
            text = text,
            packageName = sbn.packageName,
            timestamp = sbn.postTime,
        )

        Log.d(TAG, "Captured notification: pkg=${sbn.packageName} title=${title.take(10)}...")

        // Forward via WebSocket
        val webSocket = container.webSocket
        if (webSocket != null) {
            webSocket.sendNotificationBridge(data)
        } else {
            Log.w(TAG, "WebSocket not available, notification dropped")
        }
    }
}
