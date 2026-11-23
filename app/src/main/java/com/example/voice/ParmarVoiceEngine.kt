package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ai.WakeWordMatcher
import com.example.data.local.AssistantLanguage

/**
 * Manages Android SpeechRecognizer on the main thread for both:
 * 1) Offline-preferred DIAMOND wake-word standby (never streams audio continuously to custom servers)
 * 2) Active Gujarati / English command recognition
 */
class ParmarVoiceEngine(private val context: Context) {

    enum class Mode { NONE, WAKE_STANDBY, ACTIVE_COMMAND }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private var currentMode: Mode = Mode.NONE
    private var configuredWakeWord: String = "DIAMOND"
    private var activeLanguage: AssistantLanguage = AssistantLanguage.GUJARATI
    private var offlineOnly: Boolean = false
    private var restartBackoffMs: Long = 350L

    fun isRecognizerAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun hasMicPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun configure(
        wakeWord: String,
        language: AssistantLanguage,
        offlineOnly: Boolean
    ) {
        this.configuredWakeWord = wakeWord.ifBlank { "DIAMOND" }
        this.activeLanguage = language
        this.offlineOnly = offlineOnly
    }

    fun startWakeWordStandby() {
        mainHandler.post {
            if (!hasMicPermission()) {
                VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("Microphone permission required for wake word."))
                VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                return@post
            }
            if (!isRecognizerAvailable()) {
                VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("Speech recognition service unavailable on device."))
                VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                return@post
            }
            currentMode = Mode.WAKE_STANDBY
            restartBackoffMs = 350L
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.WAKE_WORD_STANDBY)
            VoiceRuntimeBus.updatePartialTranscript("")
            beginListeningCycle()
        }
    }

    fun startActiveCommandListening() {
        mainHandler.post {
            if (!hasMicPermission()) {
                VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("Microphone permission required."))
                return@post
            }
            if (!isRecognizerAvailable()) {
                VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("Speech recognition service unavailable."))
                return@post
            }
            currentMode = Mode.ACTIVE_COMMAND
            restartBackoffMs = 350L
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.ACTIVE_LISTENING)
            VoiceRuntimeBus.updatePartialTranscript("")
            beginListeningCycle()
        }
    }

    fun stopAll() {
        mainHandler.post {
            currentMode = Mode.NONE
            mainHandler.removeCallbacksAndMessages(null)
            destroyRecognizer()
            VoiceRuntimeBus.updateRms(0f)
            VoiceRuntimeBus.updatePartialTranscript("")
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
        }
    }

    private fun beginListeningCycle() {
        if (currentMode == Mode.NONE) return
        destroyRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                restartBackoffMs = 350L
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                VoiceRuntimeBus.updateRms(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                VoiceRuntimeBus.updateRms(0f)
            }

            override fun onError(error: Int) {
                VoiceRuntimeBus.updateRms(0f)
                Log.d(TAG, "SpeechRecognizer onError code=$error mode=$currentMode")
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    currentMode = Mode.NONE
                    VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                    VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("Microphone permission denied."))
                    return
                }
                if (currentMode == Mode.WAKE_STANDBY) {
                    val delay = when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 900L
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 250L
                        else -> {
                            restartBackoffMs = (restartBackoffMs * 2).coerceAtMost(4000L)
                            restartBackoffMs
                        }
                    }
                    mainHandler.postDelayed({ beginListeningCycle() }, delay)
                } else if (currentMode == Mode.ACTIVE_COMMAND) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                        VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("No speech detected, Sir. Tap the orb or say $configuredWakeWord."))
                        currentMode = Mode.NONE
                    } else {
                        VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                        currentMode = Mode.NONE
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                VoiceRuntimeBus.updateRms(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = matches.firstOrNull().orEmpty().trim()
                VoiceRuntimeBus.updatePartialTranscript(best)
                handleRecognizedText(best, matches)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val partial = matches.firstOrNull().orEmpty().trim()
                if (partial.isNotEmpty()) {
                    VoiceRuntimeBus.updatePartialTranscript(partial)
                    if (currentMode == Mode.WAKE_STANDBY &&
                        WakeWordMatcher.containsWakeWord(partial, configuredWakeWord)
                    ) {
                        val remainder = WakeWordMatcher.stripWakeWord(partial, configuredWakeWord)
                        currentMode = Mode.NONE
                        destroyRecognizer()
                        VoiceRuntimeBus.emitEvent(VoiceBusEvent.WakeWordDetected(remainder))
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = if (currentMode == Mode.WAKE_STANDBY) "en-IN" else activeLanguage.localeTag
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (currentMode == Mode.WAKE_STANDBY || offlineOnly) {
                // Keep wake-word recognition on-device whenever supported to avoid continuous cloud uploads
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        runCatching { recognizer.startListening(intent) }
            .onFailure { err ->
                Log.e(TAG, "Failed to start listening", err)
                currentMode = Mode.NONE
                VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                VoiceRuntimeBus.emitEvent(VoiceBusEvent.EngineNotice("Could not start recognizer: ${err.localizedMessage}"))
            }
    }

    private fun handleRecognizedText(best: String, allMatches: List<String>) {
        if (best.isBlank()) {
            if (currentMode == Mode.WAKE_STANDBY) {
                mainHandler.postDelayed({ beginListeningCycle() }, 250L)
            } else {
                currentMode = Mode.NONE
                VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
            }
            return
        }

        when (currentMode) {
            Mode.WAKE_STANDBY -> {
                val matchedWake = allMatches.any { WakeWordMatcher.containsWakeWord(it, configuredWakeWord) }
                if (matchedWake) {
                    val remainder = WakeWordMatcher.stripWakeWord(best, configuredWakeWord)
                    currentMode = Mode.NONE
                    VoiceRuntimeBus.emitEvent(VoiceBusEvent.WakeWordDetected(remainder))
                } else {
                    mainHandler.postDelayed({ beginListeningCycle() }, 250L)
                }
            }
            Mode.ACTIVE_COMMAND -> {
                val isStopWord = allMatches.any { candidate ->
                    WakeWordMatcher.containsWakeWord(candidate, configuredWakeWord) &&
                        WakeWordMatcher.stripWakeWord(candidate, configuredWakeWord).isEmpty()
                }
                currentMode = Mode.NONE
                if (isStopWord) {
                    VoiceRuntimeBus.emitEvent(VoiceBusEvent.StopWordDetected)
                } else {
                    VoiceRuntimeBus.emitEvent(VoiceBusEvent.CommandRecognized(best))
                }
            }
            Mode.NONE -> Unit
        }
    }

    private fun destroyRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    companion object {
        private const val TAG = "ParmarVoiceEngine"
    }
}
