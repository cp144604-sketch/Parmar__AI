package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.AiTurnOutput
import com.example.ai.AssistantActionType
import com.example.ai.OfflineCommandRouter
import com.example.ai.ParmarAiBrain
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ParmarDatabase
import com.example.data.local.ParmarSettings
import com.example.data.local.ReminderEntity
import com.example.data.local.SettingsPreferences
import com.example.data.repository.ParmarRepository
import com.example.system.ConnectivityObserver
import com.example.system.PhoneControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground microphone service enabling wake-word ("DIAMOND") standby with a transparent
 * persistent notification and immediate Stop Listening control, including headless background
 * command execution when the UI is closed or minimized.
 */
class ParmarVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceEngine: ParmarVoiceEngine? = null
    private var ttsManager: FemaleTtsManager? = null
    private var phoneControl: PhoneControlManager? = null
    private var connectivityObserver: ConnectivityObserver? = null
    private var settingsPrefs: SettingsPreferences? = null
    private var repository: ParmarRepository? = null
    private val aiBrain by lazy { ParmarAiBrain() }
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        voiceEngine = ParmarVoiceEngine(this)
        ttsManager = FemaleTtsManager(this)
        phoneControl = PhoneControlManager(this)
        connectivityObserver = ConnectivityObserver(this)
        settingsPrefs = SettingsPreferences(this)
        val db = ParmarDatabase.getInstance(this)
        repository = ParmarRepository(db.noteDao(), db.reminderDao(), db.memoryDao(), db.chatDao())
        ensureChannel()

        serviceScope.launch {
            VoiceRuntimeBus.events.collect { event ->
                // Only handle events directly in Service when MainActivity UI is not active
                if (!VoiceRuntimeBus.isUiActive.value) {
                    handleBackgroundEvent(event)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopGracefully()
                return START_NOT_STICKY
            }
            ACTION_START_STANDBY -> {
                val wakeWord = intent.getStringExtra(EXTRA_WAKE_WORD) ?: "DIAMOND"
                val langCode = intent.getStringExtra(EXTRA_LANGUAGE) ?: AssistantLanguage.GUJARATI.code
                val offlineOnly = intent.getBooleanExtra(EXTRA_OFFLINE_ONLY, false)
                val lang = if (langCode == AssistantLanguage.ENGLISH.code) {
                    AssistantLanguage.ENGLISH
                } else {
                    AssistantLanguage.GUJARATI
                }
                startForegroundWithNotification(wakeWord)
                acquirePartialWakeLock()
                VoiceRuntimeBus.setServiceRunning(true)
                voiceEngine?.configure(wakeWord, lang, offlineOnly)
                voiceEngine?.startWakeWordStandby()
            }
            ACTION_PAUSE_STANDBY -> {
                voiceEngine?.stopAll()
            }
            ACTION_RESUME_STANDBY -> {
                val wakeWord = intent.getStringExtra(EXTRA_WAKE_WORD) ?: "DIAMOND"
                val langCode = intent.getStringExtra(EXTRA_LANGUAGE) ?: AssistantLanguage.GUJARATI.code
                val offlineOnly = intent.getBooleanExtra(EXTRA_OFFLINE_ONLY, false)
                val lang = if (langCode == AssistantLanguage.ENGLISH.code) {
                    AssistantLanguage.ENGLISH
                } else {
                    AssistantLanguage.GUJARATI
                }
                voiceEngine?.configure(wakeWord, lang, offlineOnly)
                voiceEngine?.startWakeWordStandby()
            }
        }
        return START_STICKY
    }

    private suspend fun handleBackgroundEvent(event: VoiceBusEvent) {
        val settings = runCatching { settingsPrefs?.settingsFlow?.first() }.getOrNull() ?: ParmarSettings()
        val isOnline = (connectivityObserver?.isOnline?.value == true) &&
            !settings.offlineModeEnabled &&
            settings.aiCloudEnabled
        ttsManager?.updateParameters(settings.voiceSpeed, settings.voicePitch, settings.selectedFemaleVoiceName)

        when (event) {
            is VoiceBusEvent.WakeWordDetected -> {
                if (event.remainingCommand.isNotBlank()) {
                    processBackgroundCommand(event.remainingCommand, settings, isOnline)
                } else {
                    val greeting = if (settings.language == AssistantLanguage.ENGLISH) {
                        "Hello Sir! I am PARMAR. How can I help you?"
                    } else {
                        "Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?"
                    }
                    VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.SPEAKING)
                    ttsManager?.speak(greeting, settings.language.code, settings.offlineModeEnabled) {
                        voiceEngine?.configure(settings.wakeWord, settings.language, !isOnline)
                        voiceEngine?.startActiveCommandListening()
                    }
                }
            }
            VoiceBusEvent.StopWordDetected -> {
                val farewell = if (settings.language == AssistantLanguage.ENGLISH) {
                    "Session stopped, Sir."
                } else {
                    "બરાબર Sir, હું બંધ કરું છું."
                }
                ttsManager?.speak(farewell, settings.language.code, settings.offlineModeEnabled) {
                    voiceEngine?.configure(settings.wakeWord, settings.language, !isOnline)
                    voiceEngine?.startWakeWordStandby()
                }
            }
            is VoiceBusEvent.CommandRecognized -> {
                processBackgroundCommand(event.text, settings, isOnline)
            }
            is VoiceBusEvent.EngineNotice -> {
                voiceEngine?.configure(settings.wakeWord, settings.language, !isOnline)
                voiceEngine?.startWakeWordStandby()
            }
        }
    }

    private suspend fun processBackgroundCommand(
        input: String,
        settings: ParmarSettings,
        isOnline: Boolean
    ) {
        val repo = repository ?: return
        val pc = phoneControl ?: return

        repo.addChatMessage(
            ChatMessageEntity(role = "user", text = input, language = settings.language.code)
        )

        val localRoute = OfflineCommandRouter.route(
            input = input,
            preferredLanguage = settings.language,
            wakeWord = settings.wakeWord,
            isOnline = isOnline
        )

        val output = if (localRoute != null) {
            localRoute
        } else {
            VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.PROCESSING)
            val history = repo.getRecentChatHistory(10)
            val notes = repo.getNotesSnapshot()
            val reminders = repo.getRemindersSnapshot()
            val memories = repo.getMemoriesSnapshot()
            val apps = pc.getInstalledLauncherApps().map { it.label }

            withContext(Dispatchers.IO) {
                aiBrain.processTurn(
                    userMessage = input,
                    preferredLanguage = settings.language,
                    history = history,
                    notes = notes,
                    reminders = reminders,
                    memories = memories,
                    installedAppNames = apps
                )
            }.getOrElse { err ->
                AiTurnOutput(
                    reply = if (settings.language == AssistantLanguage.ENGLISH) {
                        "Sir, AI connection failed: ${err.localizedMessage ?: "Offline"}"
                    } else {
                        "Sir, AI સેવા સાથે સંપર્ક થયો નથી. ઓફલાઇન કમાન્ડ્સ ઉપલબ્ધ છે."
                    },
                    language = settings.language.code,
                    action = AssistantActionType.NONE,
                    isOffline = true
                )
            }
        }

        val useGu = output.language == "gu"
        var finalReply = output.reply
        when (output.action) {
            AssistantActionType.FLASHLIGHT_ON -> {
                val res = pc.setFlashlight(true)
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.FLASHLIGHT_OFF -> {
                val res = pc.setFlashlight(false)
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.OPEN_APP -> {
                val res = pc.openAppByName(output.actionParam)
                if (finalReply.isBlank() || !res.success) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.READ_DATE_TIME -> {
                val lang = if (useGu) AssistantLanguage.GUJARATI else AssistantLanguage.ENGLISH
                finalReply = pc.getCurrentDateTimeText(lang)
            }
            AssistantActionType.VOLUME_UP -> {
                val res = pc.adjustVolume("up")
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.VOLUME_DOWN -> {
                val res = pc.adjustVolume("down")
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.VOLUME_MUTE -> {
                val res = pc.adjustVolume("mute")
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.OPEN_BLUETOOTH_SETTINGS -> {
                val res = pc.openBluetoothSettings()
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.OPEN_WIFI_SETTINGS -> {
                val res = pc.openWifiSettings()
                if (finalReply.isBlank()) finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.LOCK_SCREEN -> {
                val res = pc.lockScreenNow()
                finalReply = if (useGu) res.messageGu else res.messageEn
            }
            AssistantActionType.CREATE_NOTE -> {
                val text = output.actionParam.ifBlank { finalReply }
                repo.saveNote(NoteEntity(title = text.take(36), content = text))
            }
            AssistantActionType.CREATE_REMINDER -> {
                val title = output.actionParam.ifBlank { "PARMAR Reminder" }
                val triggerAt = System.currentTimeMillis() + 60 * 60 * 1000L
                val id = repo.saveReminder(ReminderEntity(title = title, triggerAtMillis = triggerAt))
                pc.scheduleLocalReminderNotification(id, title, triggerAt)
            }
            else -> Unit
        }

        repo.addChatMessage(
            ChatMessageEntity(
                role = "assistant",
                text = finalReply,
                language = output.language,
                actionExecuted = output.action.takeIf { it != AssistantActionType.NONE }?.name,
                isOfflineResponse = output.isOffline
            )
        )

        VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.SPEAKING)
        ttsManager?.speak(finalReply, output.language, settings.offlineModeEnabled) {
            voiceEngine?.configure(settings.wakeWord, settings.language, !isOnline)
            voiceEngine?.startWakeWordStandby()
        }
    }

    private fun startForegroundWithNotification(wakeWord: String) {
        val notification = buildNotification(wakeWord)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(wakeWord: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ParmarVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PARMAR AI • Listening")
            .setContentText("Say \"$wakeWord\" to activate or stop • બોલો \"$wakeWord\"")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_stop_listening),
                stopPending
            )
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_service_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquirePartialWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ParmarAi::VoiceStandbyLock").apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun stopGracefully() {
        voiceEngine?.stopAll()
        ttsManager?.stop()
        releaseWakeLock()
        VoiceRuntimeBus.setServiceRunning(false)
        VoiceRuntimeBus.updateVoiceState(AssistantVoiceState.STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        voiceEngine?.stopAll()
        ttsManager?.shutdown()
        releaseWakeLock()
        serviceScope.cancel()
        VoiceRuntimeBus.setServiceRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "parmar_voice_channel"
        const val NOTIFICATION_ID = 4041

        const val ACTION_START_STANDBY = "com.example.voice.ACTION_START_STANDBY"
        const val ACTION_PAUSE_STANDBY = "com.example.voice.ACTION_PAUSE_STANDBY"
        const val ACTION_RESUME_STANDBY = "com.example.voice.ACTION_RESUME_STANDBY"
        const val ACTION_STOP = "com.example.voice.ACTION_STOP"

        const val EXTRA_WAKE_WORD = "extra_wake_word"
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_OFFLINE_ONLY = "extra_offline_only"

        fun startStandby(context: Context, wakeWord: String, languageCode: String, offlineOnly: Boolean) {
            val intent = Intent(context, ParmarVoiceService::class.java).apply {
                action = ACTION_START_STANDBY
                putExtra(EXTRA_WAKE_WORD, wakeWord)
                putExtra(EXTRA_LANGUAGE, languageCode)
                putExtra(EXTRA_OFFLINE_ONLY, offlineOnly)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopStandby(context: Context) {
            val intent = Intent(context, ParmarVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
