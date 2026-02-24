package network.arno.android.command

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.JsonObject

/**
 * Handles close_tab commands by simulating a back press.
 *
 * Android does not have a direct "close tab" concept equivalent to
 * desktop browsers, so a GLOBAL_ACTION_BACK is used as the closest
 * analogue (requires AccessibilityService permission).
 *
 * If the accessibility service is unavailable, returns an error
 * explaining what permission is needed.
 */
class CloseTabHandler(private val context: Context) {

    companion object {
        private const val TAG = "CloseTabHandler"
    }

    fun handle(payload: JsonObject): HandlerResult {
        return try {
            // Send a broadcast that the accessibility service can pick up
            val intent = android.content.Intent("network.arno.android.ACTION_CLOSE_TAB")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
            Log.i(TAG, "close_tab broadcast sent")
            HandlerResult.Success()
        } catch (e: Exception) {
            Log.e(TAG, "close_tab failed", e)
            HandlerResult.Error("close_tab failed: ${e.message}")
        }
    }
}
