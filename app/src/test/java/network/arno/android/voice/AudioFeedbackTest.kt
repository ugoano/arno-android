package network.arno.android.voice

import org.junit.Assert.*
import org.junit.Test

class AudioFeedbackTest {

    @Test
    fun `Tone enum has expected values`() {
        val tones = AudioFeedback.Tone.entries
        assertEquals(3, tones.size)
        assertTrue(tones.contains(AudioFeedback.Tone.LISTEN_START))
        assertTrue(tones.contains(AudioFeedback.Tone.LISTEN_STOP))
        assertTrue(tones.contains(AudioFeedback.Tone.WAKE_WORD_DETECTED))
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
}
