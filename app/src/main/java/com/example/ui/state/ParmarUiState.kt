package com.example.ui.state

import com.example.ai.PendingMemoryProposal
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ParmarSettings
import com.example.data.local.ReminderEntity
import com.example.system.AppLaunchInfo
import com.example.voice.AssistantVoiceState
import com.example.voice.VoiceOptionInfo

data class PermissionHealth(
    val hasMicPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val canScheduleExactAlarms: Boolean = true,
    val isDeviceAdminEnabled: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isSpeechRecognizerAvailable: Boolean = true,
    val isDeviceSecured: Boolean = false
)

data class ParmarUiState(
    val settings: ParmarSettings = ParmarSettings(),
    val isNetworkConnected: Boolean = true,
    val voiceState: AssistantVoiceState = AssistantVoiceState.IDLE,
    val rmsLevel: Float = 0f,
    val partialTranscript: String = "",
    val lastAssistantReply: String = "Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?",
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val volumePercent: Int = 70,
    val isForegroundServiceRunning: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val memories: List<MemoryEntity> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val availableVoices: List<VoiceOptionInfo> = emptyList(),
    val installedApps: List<AppLaunchInfo> = emptyList(),
    val pendingMemoryProposal: PendingMemoryProposal? = null,
    val privateMemoryUnlocked: Boolean = false,
    val permissionHealth: PermissionHealth = PermissionHealth(),
    val statusBanner: String? = null,
    val errorBanner: String? = null
)
