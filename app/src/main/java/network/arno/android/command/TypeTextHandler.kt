package network.arno.android.command

import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.arno.android.service.ArnoAccessibilityService

/**
 * Handles type_text commands by inputting text into the currently
 * focused input field using ACTION_SET_TEXT.
 *
 * Payload:
 *   - "text": String (required) — the text to type
 *
 * Requires an input field to be focused on screen.
 */
class TypeTextHandler {

    companion object {
        private const val TAG = "TypeTextHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val service = ArnoAccessibilityService.instance
            ?: return HandlerResult.Error(
                "Accessibility service not enabled. Enable it in Settings > Accessibility > Arno"
            )

        val text = payload["text"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'text' in payload")

        Log.i(TAG, "Typing text: '${text.take(20)}${if (text.length > 20) "..." else ""}'")
        return service.typeText(text)
    }
}
