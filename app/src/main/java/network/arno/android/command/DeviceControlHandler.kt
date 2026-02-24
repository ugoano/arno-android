package network.arno.android.command

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Handles generic device_control commands — the Android equivalent of
 * the CLI client's applescript command.
 *
 * Accepts an "action" string and optional "extras" object, builds an
 * Android Intent, and fires it. This allows the bridge to perform
 * arbitrary device actions without adding individual command types.
 *
 * Supported actions:
 *   - Any valid Intent action string (e.g. "android.intent.action.VIEW")
 *   - Extras are passed as string key-value pairs on the intent
 *   - Optional "data" field treated as a URI for Intent.setData()
 *
 * Example payload:
 * ```json
 * {
 *   "action": "android.intent.action.VIEW",
 *   "data": "https://example.com",
 *   "extras": { "key": "value" }
 * }
 * ```
 */
class DeviceControlHandler(private val context: Context) {

    companion object {
        private const val TAG = "DeviceControlHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        val action = payload["action"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'action' in payload")

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Set data URI if provided
                payload["data"]?.jsonPrimitive?.content?.let { uri ->
                    data = Uri.parse(uri)
                }

                // Add string extras if provided
                payload["extras"]?.jsonObject?.forEach { (key, value) ->
                    putExtra(key, value.jsonPrimitive.content)
                }
            }

            context.startActivity(intent)
            Log.i(TAG, "device_control fired action: $action")

            val resultData = buildJsonObject {
                put("action", action)
                put("status", "fired")
            }
            HandlerResult.Success(resultData)
        } catch (e: Exception) {
            Log.e(TAG, "device_control failed for action: $action", e)
            HandlerResult.Error("device_control failed: ${e.message}")
        }
    }
}
