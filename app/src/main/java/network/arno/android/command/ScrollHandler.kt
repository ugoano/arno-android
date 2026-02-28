package network.arno.android.command

import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.arno.android.service.ArnoAccessibilityService

/**
 * Handles scroll commands by finding a scrollable element on screen
 * and scrolling in the specified direction.
 *
 * Payload:
 *   - "direction": String (required) — "up", "down", "left", or "right"
 */
class ScrollHandler {

    companion object {
        private const val TAG = "ScrollHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val service = ArnoAccessibilityService.instance
            ?: return HandlerResult.Error(
                "Accessibility service not enabled. Enable it in Settings > Accessibility > Arno"
            )

        val directionStr = payload["direction"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'direction' in payload")

        val direction = ScrollDirection.fromString(directionStr)
            ?: return HandlerResult.Error(
                "Unknown scroll direction: '$directionStr'. Valid: up, down, left, right"
            )

        Log.i(TAG, "Scrolling: $directionStr")
        return service.scroll(direction)
    }
}
