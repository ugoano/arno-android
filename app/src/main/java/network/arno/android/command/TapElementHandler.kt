package network.arno.android.command

import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.arno.android.service.ArnoAccessibilityService

/**
 * Handles tap_element commands by finding a UI element matching the
 * specified text or content description and performing a click action.
 *
 * Payload:
 *   - "text": String (optional) — match element by visible text
 *   - "content_description": String (optional) — match by content description
 *
 * At least one of text or content_description must be provided.
 */
class TapElementHandler {

    companion object {
        private const val TAG = "TapElementHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val service = ArnoAccessibilityService.instance
            ?: return HandlerResult.Error(
                "Accessibility service not enabled. Enable it in Settings > Accessibility > Arno"
            )

        val text = payload["text"]?.jsonPrimitive?.content
        val contentDescription = payload["content_description"]?.jsonPrimitive?.content

        Log.i(TAG, "Tapping element: text='$text' desc='$contentDescription'")
        return service.tapElement(text, contentDescription)
    }
}
