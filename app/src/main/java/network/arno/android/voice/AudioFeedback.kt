package network.arno.android.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log

/**
 * Provides audio feedback for voice mode state changes.
 *
 * Activation tones (LISTEN_START, WAKE_WORD_DETECTED) play a custom WAV from
 * raw resources when a Context is available, falling back to ToneGenerator.
 * Feedback tones (LISTEN_STOP, SPEECH_CAPTURED) always use ToneGenerator for
 * low-latency response.
 */
class AudioFeedback(private val context: Context? = null) {

    companion object {
        private const val TAG = "AudioFeedback"
        private const val STREAM_VOLUME = 100 // 0-100, max for audibility on BT

        /** Tones that use the custom WAV when available. */
        private val ACTIVATION_TONES = setOf(Tone.LISTEN_START, Tone.WAKE_WORD_DETECTED)
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

    private var activationPlayer: MediaPlayer? = null
    private var customToneResId: Int = resolveCustomTone()

    private fun resolveCustomTone(): Int {
        val ctx = context ?: return 0
        return ctx.resources.getIdentifier("bt_activation", "raw", ctx.packageName)
    }

    /**
     * Play the given tone. Activation tones use the custom WAV when available;
     * feedback tones always use ToneGenerator.
     */
    fun play(tone: Tone) {
        if (tone in ACTIVATION_TONES && customToneResId != 0 && context != null) {
            playCustomWav()
        } else {
            playToneGenerator(tone)
        }
    }

    private fun playCustomWav() {
        try {
            activationPlayer?.release()
            activationPlayer = MediaPlayer.create(context, customToneResId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { mp -> mp.release() }
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Custom WAV failed, falling back to ToneGenerator", e)
            playToneGenerator(Tone.LISTEN_START)
        }
    }

    private fun playToneGenerator(tone: Tone) {
        try {
            toneGenerator?.startTone(tone.toneType, tone.durationMs)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play tone ${tone.name}", e)
        }
    }

    /**
     * Play the custom activation tone directly.
     * Used by BT trigger path. Falls back to ToneGenerator if unavailable.
     */
    fun playActivationTone() {
        play(Tone.LISTEN_START)
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release ToneGenerator", e)
        }
        toneGenerator = null
        try {
            activationPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release MediaPlayer", e)
        }
        activationPlayer = null
    }
}
