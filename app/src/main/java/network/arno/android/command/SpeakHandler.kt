package network.arno.android.command

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

class SpeakHandler(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SpeakHandler"
    }

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.UK
            ready = true
            Log.i(TAG, "TTS engine initialised")
        } else {
            Log.e(TAG, "TTS init failed with status $status")
        }
    }

    fun handle(payload: JsonObject): HandlerResult {
        if (!ready) return HandlerResult.Error("TTS engine not ready")

        val text = payload["text"]?.jsonPrimitive?.content
            ?: return HandlerResult.Error("Missing 'text' in payload")

        val interruptible = payload["interruptible"]?.jsonPrimitive?.boolean ?: true
        val queueMode = if (interruptible) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

        tts?.speak(text, queueMode, null, "arno_speak_${System.currentTimeMillis()}")
        return HandlerResult.Success()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
