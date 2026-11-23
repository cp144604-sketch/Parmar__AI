package com.example.voice

enum class AssistantVoiceState {
    IDLE,
    WAKE_WORD_STANDBY,
    ACTIVE_LISTENING,
    PROCESSING,
    SPEAKING,
    STOPPED
}

data class VoiceOptionInfo(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val isOfflineReady: Boolean,
    val isFemaleLikely: Boolean
)
