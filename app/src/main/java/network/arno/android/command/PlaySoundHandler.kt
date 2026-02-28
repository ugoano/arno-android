package network.arno.android.command

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Handles play_sound command — plays system tones or media URIs via MediaPlayer.
 * Works with screen off through the foreground service.
 *
 * ## Payload:
 * ```json
 * {"type": "alarm"}                              // default alarm tone
 * {"type": "notification"}                       // default notification
 * {"type": "ringtone"}                           // default ringtone
 * {"uri": "content://media/external/audio/123"}  // specific media
 * {"type": "alarm", "duration_ms": 5000}         // stop after 5s
 * {"type": "alarm", "volume": 0.5}               // half volume
 * ```
 */
class PlaySoundHandler(private val context: Context) {

    companion object {
        private const val TAG = "PlaySoundHandler"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentPlayer: MediaPlayer? = null

    fun handle(payload: JsonObject): HandlerResult {
        // Stop any currently playing sound
        stopCurrent()

        val typeStr = payload["type"]?.jsonPrimitive?.content
        val uriStr = payload["uri"]?.jsonPrimitive?.content
        val durationMs = payload["duration_ms"]?.jsonPrimitive?.long
            ?: PlaySoundConfig.PLAY_TO_COMPLETION
        val volume = PlaySoundConfig.clampVolume(
            payload["volume"]?.jsonPrimitive?.float ?: PlaySoundConfig.DEFAULT_VOLUME
        )

        // Resolve the URI to play
        val soundUri: Uri
        val soundTypeLabel: String

        if (uriStr != null) {
            soundUri = Uri.parse(uriStr)
            soundTypeLabel = "media"
        } else if (typeStr != null) {
            val soundType = SoundType.fromString(typeStr)
                ?: return HandlerResult.Error(
                    "Unknown sound type: $typeStr. Use: alarm, notification, ringtone"
                )
            soundUri = getDefaultUri(soundType)
                ?: return HandlerResult.Error("No default $typeStr tone configured on device")
            soundTypeLabel = typeStr.lowercase()
        } else {
            return HandlerResult.Error("Missing 'type' or 'uri' in payload")
        }

        return try {
            val player = MediaPlayer().apply {
                val attrs = AudioAttributes.Builder()
                    .setUsage(
                        if (soundTypeLabel == "alarm") AudioAttributes.USAGE_ALARM
                        else AudioAttributes.USAGE_MEDIA
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(attrs)
                setDataSource(context, soundUri)
                setVolume(volume, volume)
                setOnCompletionListener { mp ->
                    mp.release()
                    if (currentPlayer === mp) currentPlayer = null
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    mp.release()
                    if (currentPlayer === mp) currentPlayer = null
                    true
                }
                prepare()
                start()
            }
            currentPlayer = player

            // Schedule stop if duration specified
            if (durationMs > 0) {
                handler.postDelayed({
                    stopCurrent()
                }, durationMs)
            }

            Log.i(TAG, "Playing $soundTypeLabel (volume=$volume, duration=$durationMs)")

            val data = buildJsonObject {
                put("status", "playing")
                put("type", soundTypeLabel)
                if (durationMs > 0) put("duration_ms", durationMs)
            }
            HandlerResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound", e)
            HandlerResult.Error("Failed to play sound: ${e.message}")
        }
    }

    private fun getDefaultUri(type: SoundType): Uri? {
        val ringtoneType = when (type) {
            SoundType.ALARM -> RingtoneManager.TYPE_ALARM
            SoundType.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
            SoundType.RINGTONE -> RingtoneManager.TYPE_RINGTONE
        }
        return RingtoneManager.getDefaultUri(ringtoneType)
    }

    private fun stopCurrent() {
        currentPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping player", e)
            }
            currentPlayer = null
        }
        handler.removeCallbacksAndMessages(null)
    }
}
