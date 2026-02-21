package network.arno.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import network.arno.android.ArnoApp
import network.arno.android.MainActivity
import network.arno.android.R
import network.arno.android.chat.ChatRepository
import network.arno.android.command.CommandExecutor
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState

class ArnoService : Service() {

    companion object {
        private const val TAG = "ArnoService"
        const val SERVICE_CHANNEL_ID = "arno_service"
        const val SERVICE_CHANNEL_NAME = "Arno Connection"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var webSocket: ArnoWebSocket
    private lateinit var commandExecutor: CommandExecutor
    lateinit var chatRepository: ChatRepository
        private set

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = ArnoBinder()

    inner class ArnoBinder : Binder() {
        val service: ArnoService get() = this@ArnoService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        createServiceChannel()

        val container = (application as ArnoApp).container
        val settingsRepository = container.settingsRepository
        chatRepository = ChatRepository(
            tasksRepository = container.tasksRepository,
            messageDao = container.database.messageDao(),
            sessionDao = container.database.sessionDao(),
            scope = serviceScope,
        )
        commandExecutor = CommandExecutor(applicationContext, settingsRepository)

        val serverUrl = container.settingsRepository.serverUrl
        webSocket = ArnoWebSocket(
            serverUrl = serverUrl,
            onCommand = { cmd -> commandExecutor.execute(cmd) },
        )

        // Store references in app container for ViewModel access
        container.apply {
            this.webSocket = this@ArnoService.webSocket
            this.chatRepository = this@ArnoService.chatRepository
            this.commandExecutor = this@ArnoService.commandExecutor
        }

        startForeground(NOTIFICATION_ID, buildNotification("Connecting..."))
        webSocket.connect()

        // Monitor connection state to update notification
        serviceScope.launch {
            webSocket.connectionState.collect { state ->
                val text = when (state) {
                    is ConnectionState.Connected -> "Connected"
                    is ConnectionState.Connecting -> "Connecting..."
                    is ConnectionState.Reconnecting -> "Reconnecting (attempt ${state.attempt})..."
                    is ConnectionState.Disconnected -> "Disconnected"
                    is ConnectionState.Error -> "Error: ${state.message}"
                }
                updateNotification(text)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        serviceScope.cancel()
        webSocket.destroy()
        commandExecutor.shutdown()
        super.onDestroy()
    }

    fun getWebSocket(): ArnoWebSocket = webSocket

    private fun createServiceChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            SERVICE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent connection to Arno"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_arno_notification)
            .setContentTitle("Arno")
            .setContentText(status)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = buildNotification(status)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
