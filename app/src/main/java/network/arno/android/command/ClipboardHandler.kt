package network.arno.android.command

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ClipboardHandler(private val context: Context) {

    private val clipboardManager: ClipboardManager
        get() = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun handleCopy(payload: JsonObject): HandlerResult {
        val text = payload["text"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'text' in payload")

        // ClipboardManager must be accessed from main thread
        Handler(Looper.getMainLooper()).post {
            clipboardManager.setPrimaryClip(
                ClipData.newPlainText("arno", text)
            )
        }
        return HandlerResult.Success()
    }

    fun handlePaste(payload: JsonObject): HandlerResult {
        // Reading clipboard also needs main thread, but we need the result synchronously
        var clipText: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        Handler(Looper.getMainLooper()).post {
            val clip = clipboardManager.primaryClip
            clipText = clip?.getItemAt(0)?.text?.toString()
            latch.countDown()
        }

        latch.await(2, java.util.concurrent.TimeUnit.SECONDS)

        val text = clipText ?: ""
        return HandlerResult.Success(
            data = buildJsonObject { put("text", text) }
        )
    }
}
