package network.arno.android.voice

enum class VoiceMode(val label: String) {
    /** Default: tap mic, speak once, auto-send. */
    PUSH_TO_TALK("Push to Talk"),

    /** Continuous: mic stays hot, each utterance auto-sends, restarts listening. */
    DICTATION("Dictation"),

    /** Always-listening: waits for "Arno"/"Jarvis" wake word, then captures command. */
    WAKE_WORD("Wake Word"),
}
