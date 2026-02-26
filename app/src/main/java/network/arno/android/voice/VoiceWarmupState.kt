package network.arno.android.voice

/**
 * Tracks speech recognizer warmup lifecycle so warmup is idempotent and retry-safe.
 */
class VoiceWarmupState {
    private var isWarm = false
    private var warmUpInProgress = false

    fun beginWarmUp(): Boolean {
        if (isWarm || warmUpInProgress) return false
        warmUpInProgress = true
        return true
    }

    fun markWarmUpSuccess() {
        isWarm = true
        warmUpInProgress = false
    }

    fun markWarmUpFailure() {
        warmUpInProgress = false
    }

    fun isWarm(): Boolean = isWarm

    fun reset() {
        isWarm = false
        warmUpInProgress = false
    }
}
