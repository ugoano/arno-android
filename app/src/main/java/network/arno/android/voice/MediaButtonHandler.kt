package network.arno.android.voice

import android.view.KeyEvent

/**
 * Pure logic for determining whether a Bluetooth media button event
 * should trigger voice input. Extracted for unit testability.
 *
 * Accepted key codes:
 * - KEYCODE_HEADSETHOOK: Ray-Ban Meta temple tap, most BT headsets
 * - KEYCODE_MEDIA_PLAY: some BT devices send this on single press
 * - KEYCODE_MEDIA_PLAY_PAUSE: some BT devices send this variant
 * - KEYCODE_MEDIA_PAUSE: Pixel Buds and some BT earphones
 */
object MediaButtonHandler {

    val ACCEPTED_KEY_CODES = setOf(
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
    )

    /**
     * Returns true if this key event should trigger voice input.
     * Only responds to ACTION_DOWN for accepted key codes when enabled.
     */
    fun shouldHandle(keyCode: Int, action: Int, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (action != KeyEvent.ACTION_DOWN) return false
        return keyCode in ACCEPTED_KEY_CODES
    }
}
