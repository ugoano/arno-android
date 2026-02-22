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
        private const val STREAM_VOLUME = 100 // 0-100, max for audibility on BT
    }

    enum class Tone(val frequencyHz: Int, val durationMs: Int, val toneType: Int) {
        LISTEN_START(880, 100, ToneGenerator.TONE_PROP_BEEP),
        LISTEN_STOP(440, 150, ToneGenerator.TONE_PROP_ACK),
        WAKE_WORD_DETECTED(1200, 200, ToneGenerator.TONE_PROP_BEEP2),
        /** Short rising tone confirming speech was captured, before acting on it. */
        SPEECH_CAPTURED(660, 120, ToneGenerator.TONE_PROP_ACK),
    }

    // Use STREAM_MUSIC so tones route through Bluetooth audio (earphones/glasses)
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, STREAM_VOLUME)
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

    /**
     * Play a distinctive double-beep for BT trigger activation.
     * More noticeable than a single tone, especially through BT earphones.
     */
    fun playDoubleBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to play second beep", e)
                }
            }, 200)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play double beep", e)
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
