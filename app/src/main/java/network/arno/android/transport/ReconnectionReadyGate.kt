package network.arno.android.transport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReconnectionReadyGate(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000L
    }

    enum class StatusDecision { SHOW, SUPPRESS }

    private var reconnectStartedAtMs: Long? = null
    private var bufferedReadyMessage: String? = null

    private val _isReconnectInProgress = MutableStateFlow(false)
    val reconnectInProgress: StateFlow<Boolean> = _isReconnectInProgress

    fun onReconnectStarted() {
        reconnectStartedAtMs = nowMs()
        _isReconnectInProgress.value = true
    }

    fun onStatusMessage(text: String): StatusDecision {
        if (!text.contains("ready", ignoreCase = true)) {
            return StatusDecision.SHOW
        }

        if (!_isReconnectInProgress.value) {
            return StatusDecision.SHOW
        }

        if (isTimedOut()) {
            clearReconnectState()
            return StatusDecision.SHOW
        }

        bufferedReadyMessage = text
        return StatusDecision.SUPPRESS
    }

    fun onChatHistoryReceived() {
        bufferedReadyMessage = null
        clearReconnectState()
    }

    fun consumeTimedOutReadyMessage(): String? {
        if (!_isReconnectInProgress.value || !isTimedOut()) {
            return null
        }

        val message = bufferedReadyMessage
        bufferedReadyMessage = null
        clearReconnectState()
        return message
    }

    fun timeoutRemainingMs(): Long? {
        if (!_isReconnectInProgress.value || bufferedReadyMessage == null) return null
        val startedAt = reconnectStartedAtMs ?: return null
        val elapsed = nowMs() - startedAt
        return (timeoutMs - elapsed).coerceAtLeast(0L)
    }

    val isReconnectInProgress: Boolean
        get() = _isReconnectInProgress.value

    private fun isTimedOut(): Boolean {
        val startedAt = reconnectStartedAtMs ?: return false
        return nowMs() - startedAt > timeoutMs
    }

    private fun clearReconnectState() {
        reconnectStartedAtMs = null
        _isReconnectInProgress.value = false
    }
}
