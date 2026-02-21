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
        private const val DEFAULT_SERVER_URL = "https://chat.arno.network"
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
}
