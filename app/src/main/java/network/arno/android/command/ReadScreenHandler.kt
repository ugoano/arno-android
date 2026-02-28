package network.arno.android.command

import android.util.Log
import kotlinx.serialization.json.JsonObject
import network.arno.android.service.ArnoAccessibilityService

/**
 * Handles read_screen commands by delegating to the AccessibilityService
 * to walk the current UI tree and return it as structured text.
 *
 * Returns a JSON object with "screen_content" (text) and "node_count" (int).
 * If the accessibility service is not enabled, returns an error with guidance.
 */
class ReadScreenHandler {

    companion object {
        private const val TAG = "ReadScreenHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val service = ArnoAccessibilityService.instance
            ?: return HandlerResult.Error(
                "Accessibility service not enabled. Enable it in Settings > Accessibility > Arno"
            )

        Log.i(TAG, "Reading screen content")
        return service.readScreen()
    }
}
