package network.arno.android.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages voice input across all three modes:
 * - PUSH_TO_TALK: single utterance, auto-send, stop.
 * - DICTATION: continuous listening, accumulate text, user sends manually.
 * - WAKE_WORD: continuous listening, ignore until "Arno"/"Jarvis" detected,
 *   then send the command portion.
 */
class VoiceInputManager(
    private val context: Context,
    private val onResult: (text: String, viaVoice: Boolean) -> Unit,
    private val onTextAccumulated: ((text: String) -> Unit)? = null,
) {
    companion object {
        private const val TAG = "VoiceInputManager"
        private const val PTT_MAX_RETRIES = 1
        private val PTT_RETRYABLE_ERRORS = setOf(
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT,
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioFeedback = AudioFeedback(context)

    private var speechRecognizer: SpeechRecognizer? = null
    private var pttRetryCount = 0
    private var currentMode: VoiceMode = VoiceMode.PUSH_TO_TALK

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    /** True when in a continuous mode (dictation or wake word) and actively running. */
    private val _isContinuousActive = MutableStateFlow(false)
    val isContinuousActive: StateFlow<Boolean> = _isContinuousActive

    private var shouldRestart = false

    /** When true, the next recognition result is auto-sent (single-capture after bare wake word). */
    private var pendingSingleCapture = false

    private fun createRecognizer(): SpeechRecognizer {
        speechRecognizer?.destroy()
        return SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
        }
    }

    private fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // For continuous modes, request longer speech timeout
            if (currentMode != VoiceMode.PUSH_TO_TALK) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            Log.d(TAG, "Ready for speech (mode=$currentMode)")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            Log.w(TAG, "Speech error: $error (mode=$currentMode)")
            _isListening.value = false
            _partialText.value = ""

            // For continuous modes, restart on recoverable errors
            if (shouldRestart && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                // ERROR_NO_MATCH (7) and ERROR_SPEECH_TIMEOUT (6) are normal in continuous mode
                restartListening()
            } else if (currentMode == VoiceMode.PUSH_TO_TALK
                && pttRetryCount < PTT_MAX_RETRIES
                && error in PTT_RETRYABLE_ERRORS) {
                // Retry once for transient errors (e.g. recogniser busy from rapid destroy/create)
                pttRetryCount++
                Log.i(TAG, "PTT transient error ($error), retrying (attempt ${pttRetryCount + 1})")
                mainHandler.postDelayed({ retryPushToTalk() }, 200)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcript = matches?.firstOrNull()?.trim() ?: ""

            if (transcript.isNotBlank()) {
                handleTranscript(transcript)
            }

            _partialText.value = ""

            // Restart for continuous modes
            if (shouldRestart) {
                restartListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim() ?: ""
            if (partial.isNotBlank()) {
                _partialText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleTranscript(transcript: String) {
        when (currentMode) {
            VoiceMode.PUSH_TO_TALK -> {
                onResult(transcript, true)
            }
            VoiceMode.DICTATION -> {
                // Accumulate text — user sends manually via Send button
                Log.d(TAG, "Dictation accumulated: $transcript")
                onTextAccumulated?.invoke(transcript) ?: onResult(transcript, true)
            }
            VoiceMode.WAKE_WORD -> {
                if (pendingSingleCapture) {
                    // Previous cycle was a bare wake word — send this utterance as the command
                    Log.d(TAG, "Single-capture follow-up: $transcript")
                    pendingSingleCapture = false
                    onResult(transcript, true)
                    return
                }

                val command = WakeWordDetector.extractCommand(transcript)
                if (command != null) {
                    Log.d(TAG, "Wake word detected, command: $command")
                    audioFeedback.play(AudioFeedback.Tone.WAKE_WORD_DETECTED)
                    onResult(command, true)
                } else if (WakeWordDetector.containsWakeWord(transcript)) {
                    // Bare wake word — next capture auto-sends
                    Log.d(TAG, "Wake word detected, awaiting follow-up command")
                    audioFeedback.play(AudioFeedback.Tone.WAKE_WORD_DETECTED)
                    pendingSingleCapture = true
                } else {
                    Log.d(TAG, "No wake word in: '$transcript'")
                }
            }
        }
    }

    private fun restartListening() {
        if (!shouldRestart) return
        try {
            val recognizer = createRecognizer()
            recognizer.setRecognitionListener(recognitionListener)
            recognizer.startListening(buildIntent())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart listening", e)
            _isContinuousActive.value = false
            shouldRestart = false
        }
    }

    private fun retryPushToTalk() {
        if (currentMode != VoiceMode.PUSH_TO_TALK) return
        try {
            val recognizer = createRecognizer()
            recognizer.setRecognitionListener(recognitionListener)
            recognizer.startListening(buildIntent())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retry PTT", e)
        }
    }

    /**
     * Start listening in the given mode.
     * For PUSH_TO_TALK: single shot.
     * For DICTATION/WAKE_WORD: continuous until [stop] is called.
     */
    fun start(mode: VoiceMode) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available")
            return
        }

        currentMode = mode
        shouldRestart = mode != VoiceMode.PUSH_TO_TALK
        pendingSingleCapture = false
        pttRetryCount = 0
        _isContinuousActive.value = shouldRestart
        _partialText.value = ""

        audioFeedback.play(AudioFeedback.Tone.LISTEN_START)

        val recognizer = createRecognizer()
        recognizer.setRecognitionListener(recognitionListener)
        recognizer.startListening(buildIntent())
    }

    /** Stop all listening. */
    fun stop() {
        // Only play stop tone if actually listening or in a continuous mode
        if (_isListening.value || _isContinuousActive.value) {
            audioFeedback.play(AudioFeedback.Tone.LISTEN_STOP)
        }
        shouldRestart = false
        pendingSingleCapture = false
        _isContinuousActive.value = false
        _isListening.value = false
        _partialText.value = ""
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping speech recognizer", e)
        }
    }

    /** Toggle: if currently active in given mode, stop. Otherwise start. */
    fun toggle(mode: VoiceMode) {
        if (_isContinuousActive.value || _isListening.value) {
            stop()
        } else {
            start(mode)
        }
    }

    fun destroy() {
        stop()
        speechRecognizer?.destroy()
        speechRecognizer = null
        audioFeedback.release()
    }
}
