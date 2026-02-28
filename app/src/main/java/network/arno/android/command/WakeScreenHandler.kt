package network.arno.android.command

import android.content.Context
import android.os.PowerManager
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Handles wake_screen command — turns the display on using PowerManager
 * with ACQUIRE_CAUSES_WAKEUP flag.
 *
 * ## Payload:
 * ```json
 * {}                          // wake with default 10s timeout
 * {"duration_ms": 30000}      // wake and keep on for 30s
 * ```
 */
class WakeScreenHandler(private val context: Context) {

    companion object {
        private const val TAG = "WakeScreenHandler"
        private const val DEFAULT_DURATION_MS = 10_000L
    }

    fun handle(payload: JsonObject): HandlerResult {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wasAlreadyOn = powerManager.isInteractive

        val durationMs = payload["duration_ms"]?.jsonPrimitive?.long ?: DEFAULT_DURATION_MS

        return try {
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE,
                "arno:wake_screen"
            )
            wakeLock.acquire(durationMs)
            Log.i(TAG, "Screen woken for ${durationMs}ms (was_already_on=$wasAlreadyOn)")

            val data = buildJsonObject {
                put("status", "screen_woken")
                put("was_already_on", wasAlreadyOn)
                put("duration_ms", durationMs)
            }
            HandlerResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wake screen", e)
            HandlerResult.Error("Failed to wake screen: ${e.message}")
        }
    }
}
