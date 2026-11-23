package com.example.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceBusEvent {
    data class WakeWordDetected(val remainingCommand: String) : VoiceBusEvent
    data object StopWordDetected : VoiceBusEvent
    data class CommandRecognized(val text: String) : VoiceBusEvent
    data class EngineNotice(val message: String) : VoiceBusEvent
}

object VoiceRuntimeBus {
    private val _voiceState = MutableStateFlow(AssistantVoiceState.IDLE)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _isForegroundServiceRunning = MutableStateFlow(false)
    val isForegroundServiceRunning: StateFlow<Boolean> = _isForegroundServiceRunning.asStateFlow()

    private val _isUiActive = MutableStateFlow(false)
    val isUiActive: StateFlow<Boolean> = _isUiActive.asStateFlow()

    private val _events = MutableSharedFlow<VoiceBusEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<VoiceBusEvent> = _events.asSharedFlow()

    fun updateVoiceState(state: AssistantVoiceState) {
        _voiceState.value = state
    }

    fun updateRms(level: Float) {
        _rmsLevel.value = level.coerceIn(0f, 1f)
    }

    fun updatePartialTranscript(text: String) {
        _partialTranscript.value = text
    }

    fun setServiceRunning(running: Boolean) {
        _isForegroundServiceRunning.value = running
    }

    fun setUiActive(active: Boolean) {
        _isUiActive.value = active
    }

    fun emitEvent(event: VoiceBusEvent) {
        _events.tryEmit(event)
    }
}
