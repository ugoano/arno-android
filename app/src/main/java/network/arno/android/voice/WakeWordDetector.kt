package network.arno.android.voice

/**
 * Detects wake words ("arno", "arnaud", "jarvis") in transcribed speech
 * and extracts the command portion that follows the wake word.
 */
object WakeWordDetector {

    private val WAKE_WORDS = listOf("arno", "arnaud", "jarvis")

    /**
     * Check whether the transcript contains any known wake word.
     */
    fun containsWakeWord(transcript: String): Boolean {
        val lower = transcript.lowercase()
        return WAKE_WORDS.any { lower.contains(it) }
    }

    /**
     * Extract the command portion after the wake word.
     * Returns null if no wake word found or if no command follows the wake word.
     */
    fun extractCommand(transcript: String): String? {
        if (transcript.isBlank()) return null

        val lower = transcript.lowercase()
        val wakeWord = WAKE_WORDS.firstOrNull { lower.contains(it) } ?: return null

        val idx = lower.indexOf(wakeWord)
        val command = transcript.substring(idx + wakeWord.length)
            .trimStart(',', '.', ':', ';', '!', '?', ' ')

        return command.ifBlank { null }
    }
}
