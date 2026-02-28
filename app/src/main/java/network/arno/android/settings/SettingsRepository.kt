package network.arno.android.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "arno_settings"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SPEECH_ENABLED = "speech_enabled"
        private const val KEY_VOICE_MODE = "voice_mode"
        private const val KEY_BLUETOOTH_TRIGGER = "bluetooth_trigger_enabled"
        private const val KEY_SILENCE_TIMEOUT_SCREEN = "silence_timeout_screen"
        private const val KEY_SILENCE_TIMEOUT_BT = "silence_timeout_bt"
        private const val KEY_NOTIFICATION_BRIDGE_ENABLED = "notification_bridge_enabled"
        private const val KEY_NOTIFICATION_WHITELIST = "notification_whitelist"
        private const val DEFAULT_SERVER_URL = "https://chat.arno.network"
        const val DEFAULT_SILENCE_TIMEOUT_SCREEN = 4000L
        const val DEFAULT_SILENCE_TIMEOUT_BT = 2000L
        val DEFAULT_NOTIFICATION_WHITELIST = setOf("com.Slack")
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var speechEnabled: Boolean
        get() = prefs.getBoolean(KEY_SPEECH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SPEECH_ENABLED, value).apply()

    var voiceMode: String
        get() = prefs.getString(KEY_VOICE_MODE, "PUSH_TO_TALK") ?: "PUSH_TO_TALK"
        set(value) = prefs.edit().putString(KEY_VOICE_MODE, value).apply()

    var bluetoothTriggerEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLUETOOTH_TRIGGER, true)
        set(value) = prefs.edit().putBoolean(KEY_BLUETOOTH_TRIGGER, value).apply()

    var silenceTimeoutScreen: Long
        get() = prefs.getLong(KEY_SILENCE_TIMEOUT_SCREEN, DEFAULT_SILENCE_TIMEOUT_SCREEN)
        set(value) = prefs.edit().putLong(KEY_SILENCE_TIMEOUT_SCREEN, value).apply()

    var silenceTimeoutBt: Long
        get() = prefs.getLong(KEY_SILENCE_TIMEOUT_BT, DEFAULT_SILENCE_TIMEOUT_BT)
        set(value) = prefs.edit().putLong(KEY_SILENCE_TIMEOUT_BT, value).apply()

    var notificationBridgeEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_BRIDGE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_BRIDGE_ENABLED, value).apply()

    var notificationWhitelist: Set<String>
        get() = prefs.getStringSet(KEY_NOTIFICATION_WHITELIST, DEFAULT_NOTIFICATION_WHITELIST)
            ?: DEFAULT_NOTIFICATION_WHITELIST
        set(value) = prefs.edit().putStringSet(KEY_NOTIFICATION_WHITELIST, value).apply()
}
