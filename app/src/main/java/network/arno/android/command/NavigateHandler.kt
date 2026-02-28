package network.arno.android.command

import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.arno.android.service.ArnoAccessibilityService

/**
 * Handles navigate commands by performing global actions via
 * the AccessibilityService (back, home, recents).
 *
 * Payload:
 *   - "action": String (required) — "back", "home", or "recents"
 */
class NavigateHandler {

    companion object {
        private const val TAG = "NavigateHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val service = ArnoAccessibilityService.instance
            ?: return HandlerResult.Error(
                "Accessibility service not enabled. Enable it in Settings > Accessibility > Arno"
            )

        val actionStr = payload["action"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'action' in payload")

        val action = NavigateAction.fromString(actionStr)
            ?: return HandlerResult.Error(
                "Unknown navigate action: '$actionStr'. Valid: back, home, recents"
            )

        Log.i(TAG, "Navigating: $actionStr")
        return service.navigate(action)
    }
}
