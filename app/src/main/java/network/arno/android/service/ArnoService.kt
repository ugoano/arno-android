package network.arno.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
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
import network.arno.android.settings.SettingsRepository
import network.arno.android.transport.ArnoWebSocket
import network.arno.android.transport.ConnectionState
import network.arno.android.voice.AudioFeedback
import network.arno.android.voice.MediaButtonHandler
import network.arno.android.voice.VoiceInputManager
import network.arno.android.voice.VoiceMode

class ArnoService : Service() {

    companion object {
        private const val TAG = "ArnoService"
        const val SERVICE_CHANNEL_ID = "arno_service"
        const val SERVICE_CHANNEL_NAME = "Arno Connection"
        private const val NOTIFICATION_ID = 1
        const val ACTION_SET_VOICE_MODE = "network.arno.android.SET_VOICE_MODE"
        const val EXTRA_VOICE_MODE = "voice_mode"
    }

    private lateinit var webSocket: ArnoWebSocket
    private lateinit var commandExecutor: CommandExecutor
    lateinit var chatRepository: ChatRepository
        private set

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = ArnoBinder()

    private var voiceInputManager: VoiceInputManager? = null
    private var currentVoiceMode: VoiceMode = VoiceMode.PUSH_TO_TALK
    private var currentConnectionText: String = "Connecting..."
    private var mediaSession: MediaSessionCompat? = null
    private var btVoiceInputManager: VoiceInputManager? = null
    private val btAudioFeedback = AudioFeedback()
    private lateinit var settingsRepository: SettingsRepository

    inner class ArnoBinder : Binder() {
        val service: ArnoService get() = this@ArnoService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        createServiceChannel()

        val container = (application as ArnoApp).container
        settingsRepository = container.settingsRepository
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

        // Restore persisted voice mode
        val savedMode = try {
            VoiceMode.valueOf(settingsRepository.voiceMode)
        } catch (_: Exception) {
            VoiceMode.PUSH_TO_TALK
        }
        currentVoiceMode = savedMode

        startForeground(NOTIFICATION_ID, buildNotification("Connecting..."))
        webSocket.connect()

        // Monitor connection state to update notification
        serviceScope.launch {
            webSocket.connectionState.collect { state ->
                currentConnectionText = when (state) {
                    is ConnectionState.Connected -> "Connected"
                    is ConnectionState.Connecting -> "Connecting..."
                    is ConnectionState.Reconnecting -> "Reconnecting (attempt ${state.attempt})..."
                    is ConnectionState.Disconnected -> "Disconnected"
                    is ConnectionState.Error -> "Error: ${state.message}"
                }
                updateNotification(buildStatusText())
            }
        }

        // Start always-listening if that was the persisted mode
        if (savedMode == VoiceMode.WAKE_WORD) {
            startAlwaysListening()
        }

        // Set up MediaSession for Bluetooth media button interception
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_VOICE_MODE -> {
                val modeName = intent.getStringExtra(EXTRA_VOICE_MODE)
                val mode = try {
                    modeName?.let { VoiceMode.valueOf(it) }
                } catch (_: Exception) {
                    null
                }
                if (mode != null) {
                    setVoiceMode(mode)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        stopAlwaysListening()
        btVoiceInputManager?.destroy()
        btVoiceInputManager = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        btAudioFeedback.release()
        serviceScope.cancel()
        webSocket.destroy()
        commandExecutor.shutdown()
        super.onDestroy()
    }

    fun getWebSocket(): ArnoWebSocket = webSocket

    /**
     * Set the voice mode. When WAKE_WORD, starts background always-listening.
     * When switching away from WAKE_WORD, stops it.
     */
    fun setVoiceMode(mode: VoiceMode) {
        Log.i(TAG, "Voice mode changed: $currentVoiceMode -> $mode")
        val wasWakeWord = currentVoiceMode == VoiceMode.WAKE_WORD
        currentVoiceMode = mode

        if (mode == VoiceMode.WAKE_WORD && !wasWakeWord) {
            startAlwaysListening()
        } else if (mode != VoiceMode.WAKE_WORD && wasWakeWord) {
            stopAlwaysListening()
        }

        updateNotification(buildStatusText())
    }

    private fun startAlwaysListening() {
        if (voiceInputManager != null) return
        Log.i(TAG, "Starting always-listening (wake word mode)")

        voiceInputManager = VoiceInputManager(
            context = applicationContext,
            onResult = { command, viaVoice ->
                Log.i(TAG, "Wake word command received: $command")
                chatRepository.addUserMessage(command, viaVoice)
                webSocket.sendMessage(command, viaVoice)
            },
        )
        voiceInputManager?.start(VoiceMode.WAKE_WORD)
        updateNotification(buildStatusText())
    }

    private fun stopAlwaysListening() {
        voiceInputManager?.destroy()
        voiceInputManager = null
        updateNotification(buildStatusText())
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "ArnoMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    } ?: return super.onMediaButtonEvent(mediaButtonEvent)

                    val shouldHandle = MediaButtonHandler.shouldHandle(
                        keyCode = keyEvent.keyCode,
                        action = keyEvent.action,
                        enabled = settingsRepository.bluetoothTriggerEnabled,
                    )

                    if (shouldHandle) {
                        Log.i(TAG, "Bluetooth media button triggered voice input (keyCode=${keyEvent.keyCode})")
                        triggerBluetoothVoiceInput()
                        return true
                    }

                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            })

            // Set playback state so the session receives media button events
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f)
                    .build()
            )

            isActive = true
        }
    }

    private fun triggerBluetoothVoiceInput() {
        btAudioFeedback.play(AudioFeedback.Tone.WAKE_WORD_DETECTED)

        // If already capturing from a BT trigger, stop the current one
        if (btVoiceInputManager != null) {
            btVoiceInputManager?.destroy()
            btVoiceInputManager = null
            return
        }

        btVoiceInputManager = VoiceInputManager(
            context = applicationContext,
            onResult = { command, viaVoice ->
                Log.i(TAG, "Bluetooth voice input result: $command")
                chatRepository.addUserMessage(command, viaVoice)
                webSocket.sendMessage(command, viaVoice)
                // Clean up after single-shot capture
                btVoiceInputManager?.destroy()
                btVoiceInputManager = null
            },
        )
        btVoiceInputManager?.start(VoiceMode.PUSH_TO_TALK)
    }

    private fun buildStatusText(): String {
        val voiceSuffix = when (currentVoiceMode) {
            VoiceMode.WAKE_WORD -> " \u00B7 Always Listening"
            VoiceMode.DICTATION -> " \u00B7 Dictation"
            VoiceMode.PUSH_TO_TALK -> ""
        }
        return "$currentConnectionText$voiceSuffix"
    }

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
