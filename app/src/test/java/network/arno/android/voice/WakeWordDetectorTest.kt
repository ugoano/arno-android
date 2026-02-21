package network.arno.android.voice

import org.junit.Assert.*
import org.junit.Test

class WakeWordDetectorTest {

    @Test
    fun `extractCommand returns command when wake word at start`() {
        val result = WakeWordDetector.extractCommand("Arno what time is it")
        assertNotNull(result)
        assertEquals("what time is it", result)
    }

    @Test
    fun `extractCommand returns command when wake word in middle`() {
        val result = WakeWordDetector.extractCommand("Hey Arno check my calendar")
        assertNotNull(result)
        assertEquals("check my calendar", result)
    }

    @Test
    fun `extractCommand is case insensitive`() {
        val result = WakeWordDetector.extractCommand("ARNO do something")
        assertNotNull(result)
        assertEquals("do something", result)
    }

    @Test
    fun `extractCommand detects jarvis wake word`() {
        val result = WakeWordDetector.extractCommand("jarvis turn off the lights")
        assertNotNull(result)
        assertEquals("turn off the lights", result)
    }

    @Test
    fun `extractCommand detects arnaud variant`() {
        val result = WakeWordDetector.extractCommand("arnaud send a message")
        assertNotNull(result)
        assertEquals("send a message", result)
    }

    @Test
    fun `extractCommand strips leading punctuation after wake word`() {
        val result = WakeWordDetector.extractCommand("Arno, play some music")
        assertNotNull(result)
        assertEquals("play some music", result)
    }

    @Test
    fun `extractCommand returns null when no wake word`() {
        val result = WakeWordDetector.extractCommand("what is the weather today")
        assertNull(result)
    }

    @Test
    fun `extractCommand returns null when wake word but no command`() {
        val result = WakeWordDetector.extractCommand("Arno")
        assertNull(result)
    }

    @Test
    fun `extractCommand returns null for empty string`() {
        val result = WakeWordDetector.extractCommand("")
        assertNull(result)
    }

    @Test
    fun `extractCommand handles wake word with trailing punctuation only`() {
        val result = WakeWordDetector.extractCommand("Arno!")
        assertNull(result)
    }

    @Test
    fun `containsWakeWord returns true for arno`() {
        assertTrue(WakeWordDetector.containsWakeWord("hey arno"))
    }

    @Test
    fun `containsWakeWord returns true for jarvis`() {
        assertTrue(WakeWordDetector.containsWakeWord("jarvis help"))
    }

    @Test
    fun `containsWakeWord returns true for arnaud`() {
        assertTrue(WakeWordDetector.containsWakeWord("arnaud do this"))
    }

    @Test
    fun `containsWakeWord returns false when no wake word`() {
        assertFalse(WakeWordDetector.containsWakeWord("hello world"))
    }

    @Test
    fun `containsWakeWord is case insensitive`() {
        assertTrue(WakeWordDetector.containsWakeWord("ARNO"))
        assertTrue(WakeWordDetector.containsWakeWord("Jarvis"))
        assertTrue(WakeWordDetector.containsWakeWord("ARNAUD"))
    }
}
