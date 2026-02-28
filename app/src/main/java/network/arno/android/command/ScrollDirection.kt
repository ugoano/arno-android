package network.arno.android.command

/**
 * Maps scroll direction strings to AccessibilityNodeInfo scroll actions.
 *
 * ACTION_SCROLL_FORWARD = 4096 (0x00001000) - scroll down/right
 * ACTION_SCROLL_BACKWARD = 8192 (0x00002000) - scroll up/left
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    companion object {
        fun fromString(direction: String): ScrollDirection? {
            return entries.firstOrNull { it.name.equals(direction, ignoreCase = true) }
        }
    }
}
