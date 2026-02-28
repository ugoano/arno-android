package network.arno.android.command

/**
 * Maps navigation action strings to Android AccessibilityService global action IDs.
 *
 * Global action constants from AccessibilityService:
 * - GLOBAL_ACTION_BACK = 1
 * - GLOBAL_ACTION_HOME = 2
 * - GLOBAL_ACTION_RECENTS = 3
 */
enum class NavigateAction(val globalActionId: Int) {
    BACK(1),
    HOME(2),
    RECENTS(3);

    companion object {
        fun fromString(action: String): NavigateAction? {
            return entries.firstOrNull { it.name.equals(action, ignoreCase = true) }
        }
    }
}
