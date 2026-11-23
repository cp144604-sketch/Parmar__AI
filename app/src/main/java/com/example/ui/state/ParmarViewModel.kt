package com.example.ui.state

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiTurnOutput
import com.example.ai.AssistantActionType
import com.example.ai.OfflineCommandRouter
import com.example.ai.ParmarAiBrain
import com.example.ai.PendingMemoryProposal
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ParmarDatabase
import com.example.data.local.ParmarSettings
import com.example.data.local.ReminderEntity
import com.example.data.local.SettingsPreferences
import com.example.data.repository.ParmarRepository
import com.example.system.ConnectivityObserver
import com.example.system.DeviceAuthHelper
import com.example.system.PhoneControlManager
import com.example.voice.AssistantVoiceState
import com.example.voice.FemaleTtsManager
import com.example.voice.ParmarVoiceEngine
import com.example.voice.ParmarVoiceService
import com.example.voice.VoiceBusEvent
import com.example.voice.VoiceRuntimeBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParmarViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = ParmarDatabase.getInstance(appContext)
    private val repository = ParmarRepository(
        db.noteDao(),
        db.reminderDao(),
        db.memoryDao(),
        db.chatDao()
    )
    private val settingsPrefs = SettingsPreferences(appContext)
    private val connectivityObserver = ConnectivityObserver(appContext)
    val phoneControl = PhoneControlManager(appContext)
    private val ttsManager = FemaleTtsManager(appContext)
    private val localVoiceEngine = ParmarVoiceEngine(appContext)
    private val aiBrain = ParmarAiBrain()

    private val _isThinking = MutableStateFlow(false)
    private val _lastReply = MutableStateFlow("Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?")
    private val _pendingMemory = MutableStateFlow<PendingMemoryProposal?>(null)
    private val _privateUnlocked = MutableStateFlow(false)
    private val _permissionHealth = MutableStateFlow(readPermissionHealth())
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _errorBanner = MutableStateFlow<String?>(null)
    private val _installedApps = MutableStateFlow(phoneControl.getInstalledLauncherApps())
    private val _volumePercent = MutableStateFlow(phoneControl.getAssistantVolumePercent())

    private var activeVoiceSession = false

    private val baseDataFlow = combine(
        settingsPrefs.settingsFlow,
        connectivityObserver.isOnline,
        repository.notesFlow,
        repository.remindersFlow,
        repository.memoriesFlow
    ) { settings, isOnline, notes, reminders, memories ->
        BaseDataSnapshot(settings, isOnline, notes, reminders, memories)
    }

    private val voiceBusFlow = combine(
        VoiceRuntimeBus.voiceState,
        VoiceRuntimeBus.rmsLevel,
        VoiceRuntimeBus.partialTranscript,
        VoiceRuntimeBus.isForegroundServiceRunning,
        ttsManager.isSpeaking
    ) { state, rms, partial, serviceRunning, speaking ->
        VoiceBusSnapshot(state, rms, partial, serviceRunning, speaking)
    }

    private val localTransientFlow = combine(
        _isThinking,
        _lastReply,
        _pendingMemory,
        _privateUnlocked,
        _permissionHealth
    ) { thinking, reply, proposal, unlocked, perms ->
        TransientSnapshot(thinking, reply, proposal, unlocked, perms)
    }

    val uiState: StateFlow<ParmarUiState> = combine(
        baseDataFlow,
        voiceBusFlow,
        localTransientFlow,
        repository.chatMessagesFlow,
        combine(
            ttsManager.availableVoices,
            phoneControl.isFlashlightOn,
            _statusBanner,
            _errorBanner,
            _volumePercent
        ) { voices, flash, status, error, vol ->
            MetaSnapshot(voices, flash, status, error, vol)
        }
    ) { base, voice, trans, chats, meta ->
        ParmarUiState(
            settings = base.settings,
            isNetworkConnected = base.isOnline,
            voiceState = voice.state,
            rmsLevel = voice.rms,
            partialTranscript = voice.partial,
            lastAssistantReply = trans.lastReply,
            isThinking = trans.isThinking,
            isSpeaking = voice.isSpeaking,
            isFlashlightOn = meta.isFlashlightOn,
            volumePercent = meta.volumePercent,
            isForegroundServiceRunning = voice.isServiceRunning,
            notes = base.notes,
            reminders = base.reminders,
            memories = base.memories,
            chatMessages = chats,
            availableVoices = meta.voices,
            installedApps = _installedApps.value,
            pendingMemoryProposal = trans.pendingMemory,
            privateMemoryUnlocked = trans.privateUnlocked,
            permissionHealth = trans.permissionHealth,
            statusBanner = meta.statusBanner,
            errorBanner = meta.errorBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ParmarUiState()
    )

    init {
        VoiceRuntimeBus.setUiActive(true)
        viewModelScope.launch {
            settingsPrefs.settingsFlow.collect { settings ->
                ttsManager.updateParameters(
                    speed = settings.voiceSpeed,
                    pitch = settings.voicePitch,
                    selectedVoiceName = settings.selectedFemaleVoiceName
                )
                localVoiceEngine.configure(
                    wakeWord = settings.wakeWord,
                    language = settings.language,
                    offlineOnly = settings.offlineModeEnabled || !connectivityObserver.isOnline.value
                )
            }
        }

        viewModelScope.launch {
            VoiceRuntimeBus.events.collect { event ->
                handleVoiceBusEvent(event)
            }
        }
    }

    fun refreshSystemStatus() {
        connectivityObserver.refresh()
        _permissionHealth.value = readPermissionHealth()
        _volumePercent.value = phoneControl.getAssistantVolumePercent()
        _installedApps.value = phoneControl.getInstalledLauncherApps()
        ttsManager.refreshVoices()
    }

    private fun readPermissionHealth(): PermissionHealth {
        val mic = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return PermissionHealth(
            hasMicPermission = mic,
            hasNotificationPermission = notif,
            canScheduleExactAlarms = phoneControl.canScheduleExactAlarms(),
            isDeviceAdminEnabled = phoneControl.isDeviceAdminActive(),
            isIgnoringBatteryOptimizations = phoneControl.isIgnoringBatteryOptimizations(),
            isSpeechRecognizerAvailable = localVoiceEngine.isRecognizerAvailable(),
            isDeviceSecured = DeviceAuthHelper.isDeviceSecured(appContext)
        )
    }

    private fun handleVoiceBusEvent(event: VoiceBusEvent) {
        val currentSettings = uiState.value.settings
        when (event) {
            is VoiceBusEvent.WakeWordDetected -> {
                activeVoiceSession = true
                if (event.remainingCommand.isNotBlank()) {
                    submitPrompt(event.remainingCommand, speakReply = true)
                } else {
                    val greeting = if (currentSettings.language == AssistantLanguage.ENGLISH) {
                        "Hello Sir! I am PARMAR. How can I help you?"
                    } else {
                        "Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?"
                    }
                    _lastReply.value = greeting
                    VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.SPEAKING)
                    ttsManager.speak(
                        text = greeting,
                        languageCode = currentSettings.language.code,
                        preferOffline = currentSettings.offlineModeEnabled
                    ) {
                        if (activeVoiceSession) {
                            localVoiceEngine.startActiveCommandListening()
                        }
                    }
                }
            }
            VoiceBusEvent.StopWordDetected -> {
                stopActiveAssistantSession()
            }
            is VoiceBusEvent.CommandRecognized -> {
                submitPrompt(event.text, speakReply = true)
            }
            is VoiceBusEvent.EngineNotice -> {
                _statusBanner.value = event.message
            }
        }
    }

    fun onOrbClicked() {
        refreshSystemStatus()
        if (!_permissionHealth.value.hasMicPermission) {
            _errorBanner.value = "Sir, please grant Microphone permission first to activate voice control."
            return
        }

        when (uiState.value.voiceState) {
            AssistantVoiceState.ACTIVE_LISTENING,
            AssistantVoiceState.SPEAKING,
            AssistantVoiceState.PROCESSING -> {
                stopActiveAssistantSession()
            }
            else -> {
                if (uiState.value.isForegroundServiceRunning) {
                    appContext.startService(
                        Intent(appContext, ParmarVoiceService::class.java).apply {
                            action = ParmarVoiceService.ACTION_PAUSE_STANDBY
                        }
                    )
                }
                activeVoiceSession = true
                val settings = uiState.value.settings
                localVoiceEngine.configure(
                    wakeWord = settings.wakeWord,
                    language = settings.language,
                    offlineOnly = settings.offlineModeEnabled || !uiState.value.isNetworkConnected
                )
                localVoiceEngine.startActiveCommandListening()
            }
        }
    }

    fun startWakeWordMode() {
        refreshSystemStatus()
        if (!_permissionHealth.value.hasMicPermission) {
            _errorBanner.value = "Microphone permission is required for DIAMOND wake word listening."
            return
        }
        val settings = uiState.value.settings
        val offline = settings.offlineModeEnabled || !uiState.value.isNetworkConnected
        if (settings.backgroundListeningEnabled) {
            ParmarVoiceService.startStandby(
                appContext,
                settings.wakeWord,
                settings.language.code,
                offline
            )
            _statusBanner.value = "Background wake-word standby active. Say \"${settings.wakeWord}\"."
        } else {
            localVoiceEngine.configure(settings.wakeWord, settings.language, offline)
            localVoiceEngine.startWakeWordStandby()
            _statusBanner.value = "In-app wake-word standby active. Say \"${settings.wakeWord}\"."
        }
    }

    fun stopActiveAssistantSession() {
        activeVoiceSession = false
        ttsManager.stop()
        localVoiceEngine.stopAll()
        val settings = uiState.value.settings
        val farewell = if (settings.language == AssistantLanguage.ENGLISH) {
            "Session stopped, Sir."
        } else {
            "બરાબર Sir, હું બંધ કરું છું."
        }
        _lastReply.value = farewell
        ttsManager.speak(farewell, settings.language.code, settings.offlineModeEnabled) {
            if (settings.backgroundListeningEnabled && _permissionHealth.value.hasMicPermission) {
                ParmarVoiceService.startStandby(
                    appContext,
                    settings.wakeWord,
                    settings.language.code,
                    settings.offlineModeEnabled || !uiState.value.isNetworkConnected
                )
            } else {
                VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.STOPPED)
            }
        }
    }

    fun stopAllListeningCompletely() {
        activeVoiceSession = false
        ttsManager.stop()
        localVoiceEngine.stopAll()
        if (uiState.value.isForegroundServiceRunning) {
            ParmarVoiceService.stopStandby(appContext)
        }
        VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
        _statusBanner.value = "Microphone listening stopped."
    }

    fun submitPrompt(rawInput: String, speakReply: Boolean = false) {
        val input = rawInput.trim()
        if (input.isEmpty()) return

        viewModelScope.launch {
            _errorBanner.value = null
            val settings = uiState.value.settings
            val isOnline = uiState.value.isNetworkConnected &&
                !settings.offlineModeEnabled &&
                settings.aiCloudEnabled

            repository.addChatMessage(
                ChatMessageEntity(
                    role = "user",
                    text = input,
                    language = settings.language.code
                )
            )

            // Check offline/instant command router first
            val localRoute = OfflineCommandRouter.route(
                input = input,
                preferredLanguage = settings.language,
                wakeWord = settings.wakeWord,
                isOnline = isOnline
            )

            if (localRoute != null) {
                val finalized = executeAssistantAction(localRoute, settings.language)
                recordAndSpeakReply(finalized, speakReply, settings.offlineModeEnabled)
                return@launch
            }

            // Online Gemini AI Brain
            _isThinking.value = true
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.PROCESSING)

            val history = repository.getRecentChatHistory(12)
            val notes = repository.getNotesSnapshot()
            val reminders = repository.getRemindersSnapshot()
            val memories = repository.getMemoriesSnapshot()
            val apps = _installedApps.value.map { it.label }

            val aiResult = withContext(Dispatchers.IO) {
                aiBrain.processTurn(
                    userMessage = input,
                    preferredLanguage = settings.language,
                    history = history,
                    notes = notes,
                    reminders = reminders,
                    memories = memories,
                    installedAppNames = apps
                )
            }
            _isThinking.value = false

            aiResult.onSuccess { output ->
                val finalized = executeAssistantAction(output, settings.language)
                recordAndSpeakReply(finalized, speakReply, settings.offlineModeEnabled)
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "AI Brain error occurred."
                _errorBanner.value = "AI Error: $errMsg"
                val honestFallback = if (settings.language == AssistantLanguage.ENGLISH) {
                    "Sir, I could not reach the Gemini AI service ($errMsg). Offline phone commands remain available."
                } else {
                    "Sir, હાલ Gemini AI સેવા સાથે સંપર્ક થઈ શક્યો નથી ($errMsg). ઓફલાઇન ફોન કમાન્ડ્સ ઉપલબ્ધ છે."
                }
                repository.addChatMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        text = honestFallback,
                        language = settings.language.code,
                        isOfflineResponse = true
                    )
                )
                _lastReply.value = honestFallback
                VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
            }
        }
    }

    private suspend fun executeAssistantAction(
        output: AiTurnOutput,
        defaultLang: AssistantLanguage
    ): AiTurnOutput {
        val useGu = (output.language.ifBlank { defaultLang.code }) == "gu"
        var replyText = output.reply
        var actionTag: String? = output.action.name.takeIf { output.action != AssistantActionType.NONE }

        when (output.action) {
            AssistantActionType.FLASHLIGHT_ON -> {
                val res = phoneControl.setFlashlight(true)
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.FLASHLIGHT_OFF -> {
                val res = phoneControl.setFlashlight(false)
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.OPEN_APP -> {
                val res = phoneControl.openAppByName(output.actionParam)
                replyText = if (replyText.isBlank() || !res.success) {
                    if (useGu) res.messageGu else res.messageEn
                } else replyText
            }
            AssistantActionType.READ_DATE_TIME -> {
                val lang = if (useGu) AssistantLanguage.GUJARATI else AssistantLanguage.ENGLISH
                replyText = phoneControl.getCurrentDateTimeText(lang)
            }
            AssistantActionType.VOLUME_UP -> {
                val res = phoneControl.adjustVolume("up")
                _volumePercent.value = phoneControl.getAssistantVolumePercent()
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.VOLUME_DOWN -> {
                val res = phoneControl.adjustVolume("down")
                _volumePercent.value = phoneControl.getAssistantVolumePercent()
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.VOLUME_MUTE -> {
                val res = phoneControl.adjustVolume("mute")
                _volumePercent.value = phoneControl.getAssistantVolumePercent()
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.OPEN_BLUETOOTH_SETTINGS -> {
                val res = phoneControl.openBluetoothSettings()
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.OPEN_WIFI_SETTINGS -> {
                val res = phoneControl.openWifiSettings()
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.LOCK_SCREEN -> {
                val res = phoneControl.lockScreenNow()
                replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.SET_ALARM -> {
                val parts = output.actionParam.split("|")
                val timeParts = parts.firstOrNull().orEmpty().split(":")
                val hour = timeParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 7
                val min = timeParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                val label = parts.getOrNull(1)?.trim().orEmpty()
                val res = phoneControl.setSystemAlarm(hour, min, label)
                if (replyText.isBlank()) replyText = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.CREATE_NOTE -> {
                val content = output.actionParam.ifBlank { replyText }
                val title = content.take(36)
                repository.saveNote(NoteEntity(title = title, content = content))
            }
            AssistantActionType.LIST_NOTES -> {
                val notes = repository.getNotesSnapshot()
                if (notes.isEmpty()) {
                    replyText = if (useGu) "Sir, હાલ કોઈ નોટ્સ સેવ કરેલી નથી." else "Sir, you have no saved notes yet."
                } else {
                    val summary = notes.take(4).joinToString(" • ") { it.title }
                    replyText = if (useGu) "Sir, તમારી નોટ્સ: $summary" else "Sir, your notes: $summary"
                }
            }
            AssistantActionType.CREATE_REMINDER -> {
                val title = output.actionParam.ifBlank { "PARMAR Reminder" }
                val triggerAt = System.currentTimeMillis() + 60 * 60 * 1000L // default +1 hr
                val id = repository.saveReminder(ReminderEntity(title = title, triggerAtMillis = triggerAt))
                phoneControl.scheduleLocalReminderNotification(id, title, triggerAt)
            }
            AssistantActionType.LIST_REMINDERS -> {
                val reminders = repository.getRemindersSnapshot().filter { !it.isCompleted }
                val notes = repository.getNotesSnapshot()
                val remSummary = if (reminders.isEmpty()) {
                    if (useGu) "કોઈ બાકી રિમાઇન્ડર નથી" else "no pending reminders"
                } else {
                    reminders.take(4).joinToString(", ") { it.title }
                }
                val noteCount = notes.size
                replyText = if (useGu) {
                    "Sir, તમારા બાકી રિમાઇન્ડર: $remSummary. અને તમારી પાસે $noteCount લોકલ નોટ્સ સેવ છે."
                } else {
                    "Sir, your pending reminders: $remSummary. You also have $noteCount saved notes."
                }
            }
            AssistantActionType.PROPOSE_MEMORY -> {
                val raw = output.actionParam
                val idx = raw.indexOf('=')
                if (idx > 0) {
                    val key = raw.substring(0, idx).trim()
                    val value = raw.substring(idx + 1).trim()
                    _pendingMemory.value = PendingMemoryProposal(keyLabel = key, valueText = value)
                } else if (raw.isNotBlank()) {
                    _pendingMemory.value = PendingMemoryProposal(keyLabel = "Note", valueText = raw)
                }
            }
            AssistantActionType.STOP_SESSION -> {
                activeVoiceSession = false
                localVoiceEngine.stopAll()
            }
            AssistantActionType.NONE -> {
                actionTag = null
            }
        }

        return output.copy(
            reply = replyText,
            action = if (actionTag == null) AssistantActionType.NONE else output.action
        )
    }

    private suspend fun recordAndSpeakReply(
        output: AiTurnOutput,
        speakReply: Boolean,
        preferOffline: Boolean
    ) {
        repository.addChatMessage(
            ChatMessageEntity(
                role = "assistant",
                text = output.reply,
                language = output.language,
                actionExecuted = output.action.takeIf { it != AssistantActionType.NONE }?.name,
                isOfflineResponse = output.isOffline
            )
        )
        _lastReply.value = output.reply

        if (output.action == AssistantActionType.STOP_SESSION) {
            stopActiveAssistantSession()
            return
        }

        if (speakReply) {
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.SPEAKING)
            ttsManager.speak(output.reply, output.language, preferOffline) {
                if (activeVoiceSession && uiState.value.settings.backgroundListeningEnabled) {
                    val s = uiState.value.settings
                    ParmarVoiceService.startStandby(
                        appContext,
                        s.wakeWord,
                        s.language.code,
                        s.offlineModeEnabled || !uiState.value.isNetworkConnected
                    )
                } else {
                    VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
                    activeVoiceSession = false
                }
            }
        } else {
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.IDLE)
        }
    }

    // Memory approval banner actions
    fun approvePendingMemory(isPrivate: Boolean = false) {
        val proposal = _pendingMemory.value ?: return
        viewModelScope.launch {
            repository.saveMemory(
                MemoryEntity(
                    keyLabel = proposal.keyLabel,
                    valueText = proposal.valueText,
                    category = proposal.category,
                    isPrivate = isPrivate,
                    approvedByUser = true
                )
            )
            _pendingMemory.value = null
            _statusBanner.value = "Saved to PARMAR Memory."
        }
    }

    fun dismissPendingMemory() {
        _pendingMemory.value = null
    }

    // Notes CRUD
    fun saveNote(id: Long = 0L, title: String, content: String, isPinned: Boolean = false) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.saveNote(
                NoteEntity(
                    id = id,
                    title = title.ifBlank { content.take(32) },
                    content = content,
                    isPinned = isPinned
                )
            )
        }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    // Reminders CRUD
    fun saveReminder(id: Long = 0L, title: String, triggerAtMillis: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val savedId = repository.saveReminder(
                ReminderEntity(
                    id = id,
                    title = title,
                    triggerAtMillis = triggerAtMillis
                )
            )
            phoneControl.scheduleLocalReminderNotification(savedId, title, triggerAtMillis)
            _statusBanner.value = "Reminder scheduled."
        }
    }

    fun toggleReminderComplete(reminder: ReminderEntity) {
        viewModelScope.launch {
            val next = !reminder.isCompleted
            repository.setReminderCompleted(reminder.id, next)
            if (next) {
                phoneControl.cancelLocalReminderNotification(reminder.id)
            } else {
                phoneControl.scheduleLocalReminderNotification(
                    reminder.id,
                    reminder.title,
                    reminder.triggerAtMillis
                )
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            phoneControl.cancelLocalReminderNotification(id)
            repository.deleteReminder(id)
        }
    }

    // Memory CRUD & Privacy Lock
    fun saveMemory(
        id: Long = 0L,
        keyLabel: String,
        valueText: String,
        category: String,
        isPrivate: Boolean
    ) {
        if (keyLabel.isBlank() || valueText.isBlank()) return
        viewModelScope.launch {
            repository.saveMemory(
                MemoryEntity(
                    id = id,
                    keyLabel = keyLabel.trim(),
                    valueText = valueText.trim(),
                    category = category,
                    isPrivate = isPrivate,
                    approvedByUser = true
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun requestUnlockPrivateMemory(activityContext: Context) {
        if (!uiState.value.settings.requireDeviceAuthForSensitive) {
            _privateUnlocked.value = true
            return
        }
        if (!DeviceAuthHelper.isDeviceSecured(activityContext)) {
            _privateUnlocked.value = true
            _statusBanner.value = "No system lock screen set — showing private entries."
            return
        }
        DeviceAuthHelper.authenticate(
            context = activityContext,
            title = "Unlock Private Memory",
            subtitle = "Verify with fingerprint or device PIN, Sir",
            onSuccess = { _privateUnlocked.value = true },
            onError = { msg -> _errorBanner.value = msg }
        )
    }

    fun lockPrivateMemory() {
        _privateUnlocked.value = false
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        repository.exportBackupJson()
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.restoreFromBackupJson(rawJson) }
            }.onSuccess { (n, r, m) ->
                _statusBanner.value = "Restored $n notes, $r reminders, and $m memories."
            }.onFailure { err ->
                _errorBanner.value = "Backup restore failed: ${err.localizedMessage}"
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _statusBanner.value = "Conversation history cleared."
        }
    }

    // Settings mutators
    fun updateWakeWord(word: String) = viewModelScope.launch { settingsPrefs.setWakeWord(word) }
    fun updateLanguage(language: AssistantLanguage) = viewModelScope.launch { settingsPrefs.setLanguage(language) }
    fun updateVoiceSpeed(speed: Float) = viewModelScope.launch { settingsPrefs.setVoiceSpeed(speed) }
    fun updateVoicePitch(pitch: Float) = viewModelScope.launch { settingsPrefs.setVoicePitch(pitch) }
    fun updateFemaleVoice(voiceName: String) = viewModelScope.launch { settingsPrefs.setSelectedFemaleVoice(voiceName) }
    fun updateOfflineMode(enabled: Boolean) = viewModelScope.launch { settingsPrefs.setOfflineMode(enabled) }
    fun updateAiCloudEnabled(enabled: Boolean) = viewModelScope.launch { settingsPrefs.setAiCloudEnabled(enabled) }
    fun updateVoiceProfile(enabled: Boolean, tag: String) = viewModelScope.launch { settingsPrefs.setVoiceProfile(enabled, tag) }
    fun updateRequireDeviceAuth(enabled: Boolean) = viewModelScope.launch { settingsPrefs.setRequireDeviceAuth(enabled) }

    fun updateBackgroundListening(enabled: Boolean) {
        viewModelScope.launch {
            settingsPrefs.setBackgroundListening(enabled)
            val s = uiState.value.settings
            if (enabled) {
                if (_permissionHealth.value.hasMicPermission) {
                    ParmarVoiceService.startStandby(
                        appContext,
                        s.wakeWord,
                        s.language.code,
                        s.offlineModeEnabled || !uiState.value.isNetworkConnected
                    )
                    _statusBanner.value = "Background DIAMOND listening service started."
                } else {
                    _errorBanner.value = "Grant Microphone permission before enabling background listening."
                }
            } else {
                ParmarVoiceService.stopStandby(appContext)
                localVoiceEngine.stopAll()
                _statusBanner.value = "Background listening disabled."
            }
        }
    }

    fun testFemaleVoice() {
        val s = uiState.value.settings
        val sample = if (s.language == AssistantLanguage.ENGLISH) {
            "Hello Sir! I am PARMAR, your personal voice assistant."
        } else {
            "Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?"
        }
        ttsManager.speak(sample, s.language.code, s.offlineModeEnabled)
    }

    fun setAssistantVolume(percent: Int) {
        phoneControl.setVolumePercent(percent)
        _volumePercent.value = phoneControl.getAssistantVolumePercent()
    }

    fun executeQuickAction(action: AssistantActionType) {
        viewModelScope.launch {
            val res = when (action) {
                AssistantActionType.FLASHLIGHT_ON -> phoneControl.setFlashlight(!uiState.value.isFlashlightOn)
                AssistantActionType.READ_DATE_TIME -> {
                    val text = phoneControl.getCurrentDateTimeText(uiState.value.settings.language)
                    _lastReply.value = text
                    ttsManager.speak(text, uiState.value.settings.language.code, uiState.value.settings.offlineModeEnabled)
                    null
                }
                AssistantActionType.OPEN_BLUETOOTH_SETTINGS -> phoneControl.openBluetoothSettings()
                AssistantActionType.OPEN_WIFI_SETTINGS -> phoneControl.openWifiSettings()
                AssistantActionType.LOCK_SCREEN -> phoneControl.lockScreenNow()
                else -> null
            }
            res?.let {
                val msg = if (uiState.value.settings.language == AssistantLanguage.GUJARATI) it.messageGu else it.messageEn
                _statusBanner.value = msg
            }
        }
    }

    fun clearBanners() {
        _statusBanner.value = null
        _errorBanner.value = null
    }

    override fun onCleared() {
        VoiceRuntimeBus.setUiActive(false)
        localVoiceEngine.stopAll()
        ttsManager.shutdown()
        super.onCleared()
    }

    private data class BaseDataSnapshot(
        val settings: ParmarSettings,
        val isOnline: Boolean,
        val notes: List<NoteEntity>,
        val reminders: List<ReminderEntity>,
        val memories: List<MemoryEntity>
    )

    private data class VoiceBusSnapshot(
        val state: AssistantVoiceState,
        val rms: Float,
        val partial: String,
        val isServiceRunning: Boolean,
        val isSpeaking: Boolean
    )

    private data class TransientSnapshot(
        val isThinking: Boolean,
        val lastReply: String,
        val pendingMemory: PendingMemoryProposal?,
        val privateUnlocked: Boolean,
        val permissionHealth: PermissionHealth
    )

    private data class MetaSnapshot(
        val voices: List<com.example.voice.VoiceOptionInfo>,
        val isFlashlightOn: Boolean,
        val statusBanner: String?,
        val errorBanner: String?,
        val volumePercent: Int
    )
}
