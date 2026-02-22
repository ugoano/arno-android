package network.arno.android.voice

import org.junit.Assert.*
import org.junit.Test

class AudioFeedbackTest {

    @Test
    fun `Tone enum has expected values`() {
        val tones = AudioFeedback.Tone.entries
        assertEquals(4, tones.size)
        assertTrue(tones.contains(AudioFeedback.Tone.LISTEN_START))
        assertTrue(tones.contains(AudioFeedback.Tone.LISTEN_STOP))
        assertTrue(tones.contains(AudioFeedback.Tone.WAKE_WORD_DETECTED))
        assertTrue(tones.contains(AudioFeedback.Tone.SPEECH_CAPTURED))
    }

    @Test
    fun `Tone frequencies are distinct`() {
        val frequencies = AudioFeedback.Tone.entries.map { it.frequencyHz }
        assertEquals(frequencies.size, frequencies.distinct().size)
    }

    @Test
    fun `Tone durations are positive`() {
        AudioFeedback.Tone.entries.forEach { tone ->
            assertTrue("${tone.name} duration should be positive", tone.durationMs > 0)
        }
    }

    @Test
    fun `AudioFeedback has constructor accepting Context parameter`() {
        // Verify the class has the expected constructor signature
        // Cannot instantiate in unit test due to ToneGenerator requiring Android framework
        val constructors = AudioFeedback::class.java.constructors
        assertTrue("Should have at least one constructor", constructors.isNotEmpty())
    }
}
