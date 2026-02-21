package network.arno.android.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.arno.android.voice.VoiceMode

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _serverUrl = MutableStateFlow(settingsRepository.serverUrl)
    val serverUrl: StateFlow<String> = _serverUrl

    private val _speechEnabled = MutableStateFlow(settingsRepository.speechEnabled)
    val speechEnabled: StateFlow<Boolean> = _speechEnabled

    private val _voiceMode = MutableStateFlow(
        try { VoiceMode.valueOf(settingsRepository.voiceMode) } catch (_: Exception) { VoiceMode.PUSH_TO_TALK }
    )
    val voiceMode: StateFlow<VoiceMode> = _voiceMode

    private val _bluetoothTriggerEnabled = MutableStateFlow(settingsRepository.bluetoothTriggerEnabled)
    val bluetoothTriggerEnabled: StateFlow<Boolean> = _bluetoothTriggerEnabled

    fun updateServerUrl(url: String) {
        settingsRepository.serverUrl = url
        _serverUrl.value = url
    }

    fun toggleSpeech(): Boolean {
        val newValue = !_speechEnabled.value
        settingsRepository.speechEnabled = newValue
        _speechEnabled.value = newValue
        return newValue
    }

    fun setVoiceMode(mode: VoiceMode) {
        settingsRepository.voiceMode = mode.name
        _voiceMode.value = mode
    }

    fun toggleBluetoothTrigger(): Boolean {
        val newValue = !_bluetoothTriggerEnabled.value
        settingsRepository.bluetoothTriggerEnabled = newValue
        _bluetoothTriggerEnabled.value = newValue
        return newValue
    }

    fun cycleVoiceMode(): VoiceMode {
        val modes = VoiceMode.entries
        val nextIndex = (modes.indexOf(_voiceMode.value) + 1) % modes.size
        val next = modes[nextIndex]
        setVoiceMode(next)
        return next
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
