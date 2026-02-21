package network.arno.android.command

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class LinkHandler(private val context: Context) {

    fun handle(payload: JsonObject): HandlerResult {
        val url = payload["url"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'url' in payload")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return HandlerResult.Success()
    }
}
