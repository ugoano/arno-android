package network.arno.android.voice

import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test

class MediaButtonHandlerTest {

    @Test
    fun `shouldHandle returns true for KEYCODE_HEADSETHOOK ACTION_DOWN when enabled`() {
        assertTrue(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_HEADSETHOOK,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns true for KEYCODE_MEDIA_PLAY ACTION_DOWN when enabled`() {
        assertTrue(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns true for KEYCODE_MEDIA_PLAY_PAUSE ACTION_DOWN when enabled`() {
        assertTrue(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns true for KEYCODE_MEDIA_PAUSE ACTION_DOWN when enabled`() {
        assertTrue(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns false for ACTION_UP`() {
        assertFalse(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_HEADSETHOOK,
                action = KeyEvent.ACTION_UP,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns false when disabled`() {
        assertFalse(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_HEADSETHOOK,
                action = KeyEvent.ACTION_DOWN,
                enabled = false,
            )
        )
    }

    @Test
    fun `shouldHandle returns false for non-media key codes`() {
        assertFalse(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns false for KEYCODE_MEDIA_STOP`() {
        assertFalse(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_MEDIA_STOP,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns false for KEYCODE_MEDIA_NEXT`() {
        assertFalse(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_MEDIA_NEXT,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `shouldHandle returns false for KEYCODE_MEDIA_PREVIOUS`() {
        assertFalse(
            MediaButtonHandler.shouldHandle(
                keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                action = KeyEvent.ACTION_DOWN,
                enabled = true,
            )
        )
    }

    @Test
    fun `ACCEPTED_KEY_CODES contains expected codes`() {
        val accepted = MediaButtonHandler.ACCEPTED_KEY_CODES
        assertTrue(accepted.contains(KeyEvent.KEYCODE_HEADSETHOOK))
        assertTrue(accepted.contains(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertTrue(accepted.contains(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertTrue(accepted.contains(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertEquals(4, accepted.size)
    }
}
