package network.arno.android.voice

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Provides audio feedback for voice mode state changes.
 * Uses ToneGenerator for low-latency beeps.
 */
class AudioFeedback {

    companion object {
        private const val TAG = "AudioFeedback"
        private const val STREAM_VOLUME = 80 // 0-100
    }

    enum class Tone(val frequencyHz: Int, val durationMs: Int, val toneType: Int) {
        LISTEN_START(880, 100, ToneGenerator.TONE_PROP_BEEP),
        LISTEN_STOP(440, 150, ToneGenerator.TONE_PROP_ACK),
        WAKE_WORD_DETECTED(1200, 200, ToneGenerator.TONE_PROP_BEEP2),
    }

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, STREAM_VOLUME)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to create ToneGenerator", e)
        null
    }

    fun play(tone: Tone) {
        try {
            toneGenerator?.startTone(tone.toneType, tone.durationMs)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play tone ${tone.name}", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release ToneGenerator", e)
        }
        toneGenerator = null
    }
}
