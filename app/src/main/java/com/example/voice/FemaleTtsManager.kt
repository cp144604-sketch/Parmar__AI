package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class FemaleTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var isReady = false

    private val _availableVoices = MutableStateFlow<List<VoiceOptionInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOptionInfo>> = _availableVoices.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentSpeed = 1.0f
    private var currentPitch = 1.08f
    private var preferredVoiceName = ""
    private var onCompleteCallback: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onCompleteCallback?.invoke()
                    onCompleteCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onCompleteCallback?.invoke()
                    onCompleteCallback = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onCompleteCallback?.invoke()
                    onCompleteCallback = null
                }
            })
            refreshVoices()
        }
    }

    fun refreshVoices() {
        if (!isReady) return
        val voices = runCatching { tts?.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val filtered = voices
            .filter { voice ->
                val lang = voice.locale.language.lowercase(Locale.ROOT)
                lang == "gu" || lang == "en"
            }
            .map { voice ->
                val lower = voice.name.lowercase(Locale.ROOT)
                val femaleHint = lower.contains("female") ||
                    lower.contains("-f-") ||
                    lower.contains("guf") ||
                    lower.contains("ene") ||
                    lower.contains("sfg") ||
                    lower.contains("iob") ||
                    lower.contains("tpc") ||
                    lower.contains("samantha") ||
                    lower.contains("karen") ||
                    lower.contains("veena") ||
                    lower.contains("lekha")
                val offline = !voice.isNetworkConnectionRequired
                val tag = "${voice.locale.language}-${voice.locale.country}"
                VoiceOptionInfo(
                    name = voice.name,
                    displayLabel = "${voice.locale.displayName} • ${if (femaleHint) "Female" else "Voice"}${if (offline) " (Offline)" else ""}",
                    localeTag = tag,
                    isOfflineReady = offline,
                    isFemaleLikely = femaleHint
                )
            }
            .sortedWith(compareByDescending<VoiceOptionInfo> { it.isFemaleLikely }.thenBy { it.localeTag })
        _availableVoices.value = filtered
    }

    fun updateParameters(speed: Float, pitch: Float, selectedVoiceName: String) {
        currentSpeed = speed.coerceIn(0.6f, 1.6f)
        currentPitch = pitch.coerceIn(0.8f, 1.4f)
        preferredVoiceName = selectedVoiceName
    }

    fun speak(
        text: String,
        languageCode: String,
        preferOffline: Boolean = false,
        onDone: (() -> Unit)? = null
    ) {
        val engine = tts
        if (!isReady || engine == null || text.isBlank()) {
            onDone?.invoke()
            return
        }

        onCompleteCallback = onDone
        val targetLocale = if (languageCode == "en") Locale("en", "IN") else Locale("gu", "IN")
        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (languageCode == "gu") {
                engine.setLanguage(Locale("gu"))
            } else {
                engine.setLanguage(Locale.US)
            }
        }

        selectBestFemaleVoice(engine, targetLocale, preferOffline)

        engine.setSpeechRate(currentSpeed)
        engine.setPitch(currentPitch)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun selectBestFemaleVoice(
        engine: TextToSpeech,
        targetLocale: Locale,
        preferOffline: Boolean
    ) {
        val voices: List<Voice> = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        if (voices.isEmpty()) return

        if (preferredVoiceName.isNotBlank()) {
            val chosen = voices.firstOrNull { it.name == preferredVoiceName }
            if (chosen != null && chosen.locale.language == targetLocale.language) {
                engine.voice = chosen
                return
            }
        }

        val matchingLang = voices.filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) }
        val pool = matchingLang.ifEmpty { voices }
        val ranked = pool.sortedWith(
            compareByDescending<Voice> { isFemaleVoice(it) }
                .thenByDescending { !preferOffline || !it.isNetworkConnectionRequired }
                .thenByDescending { it.locale.country.equals("IN", ignoreCase = true) }
                .thenBy { it.latency }
        )
        ranked.firstOrNull()?.let { engine.voice = it }
    }

    private fun isFemaleVoice(voice: Voice): Boolean {
        val name = voice.name.lowercase(Locale.ROOT)
        return name.contains("female") ||
            name.contains("-f-") ||
            name.contains("guf") ||
            name.contains("ene") ||
            name.contains("sfg") ||
            name.contains("iob") ||
            name.contains("veena") ||
            name.contains("lekha") ||
            name.contains("samantha")
    }

    fun stop() {
        onCompleteCallback = null
        _isSpeaking.value = false
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        isReady = false
    }
}
