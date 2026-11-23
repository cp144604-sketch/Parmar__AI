package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ai.AssistantActionType
import com.example.data.local.AssistantLanguage
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarGlowingOrb
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.components.QuickActionTile
import com.example.ui.components.StatusBannerBar
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import com.example.voice.AssistantVoiceState

@Composable
fun HomeScreen(
    uiState: ParmarUiState,
    onOrbClick: () -> Unit,
    onStartWakeStandby: () -> Unit,
    onStopListening: () -> Unit,
    onQuickAction: (AssistantActionType) -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val isGu = uiState.settings.language == AssistantLanguage.GUJARATI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VoidBlack, DeepCobalt.copy(alpha = 0.22f), VoidBlack)
                )
            )
    ) {
        ParmarTelemetryHeader(
            title = "PARMAR AI",
            voiceState = uiState.voiceState,
            wakeWord = uiState.settings.wakeWord,
            isOnline = uiState.isNetworkConnected,
            isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
            onStopListening = onStopListening,
            trailingActions = {
                IconButton(
                    onClick = onNavigateToPermissions,
                    modifier = Modifier.testTag("home_permissions_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Permissions and Privacy",
                        tint = ElectricCyan
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("home_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = IceWhite
                    )
                }
            }
        )

        StatusBannerBar(
            statusMessage = uiState.statusBanner,
            errorMessage = uiState.errorBanner,
            onDismiss = onDismissBanner
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ParmarGlowingOrb(
                        voiceState = uiState.voiceState,
                        rmsLevel = uiState.rmsLevel,
                        isOfflineMode = !uiState.isNetworkConnected || uiState.settings.offlineModeEnabled,
                        orbSize = 210.dp,
                        onClick = onOrbClick
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val promptHint = when (uiState.voiceState) {
                        AssistantVoiceState.WAKE_WORD_STANDBY ->
                            if (isGu) "બોલો \"${uiState.settings.wakeWord}\" શરૂ કરવા માટે"
                            else "Say \"${uiState.settings.wakeWord}\" to activate"
                        AssistantVoiceState.ACTIVE_LISTENING ->
                            if (isGu) "PARMAR સાંભળી રહી છે, Sir... (બંધ કરવા ${uiState.settings.wakeWord} બોલો)"
                            else "Listening, Sir... (Say ${uiState.settings.wakeWord} to stop)"
                        AssistantVoiceState.PROCESSING ->
                            if (isGu) "PARMAR વિચારી રહી છે..." else "PARMAR is thinking..."
                        AssistantVoiceState.SPEAKING ->
                            if (isGu) "PARMAR જવાબ આપી રહી છે..." else "PARMAR is speaking..."
                        else ->
                            if (isGu) "ઓર્બ પર ટેપ કરો અથવા \"${uiState.settings.wakeWord}\" સ્ટેન્ડબાય શરૂ કરો"
                            else "Tap the orb or start \"${uiState.settings.wakeWord}\" standby"
                    }

                    Text(
                        text = promptHint,
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan,
                        textAlign = TextAlign.Center
                    )

                    if (uiState.partialTranscript.isNotBlank()) {
                        Text(
                            text = "\"${uiState.partialTranscript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onStartWakeStandby,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalElectricBlue,
                                contentColor = IceWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("start_diamond_standby_btn")
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGu) "${uiState.settings.wakeWord} ચાલુ કરો" else "${uiState.settings.wakeWord} Standby",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onStopListening,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_stop_btn")
                        ) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = null,
                                tint = CrimsonAlert,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGu) "સાંભળવાનું બંધ કરો" else "Stop Listening",
                                color = IceWhite
                            )
                        }
                    }
                }
            }

            item {
                ParmarGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricCyan.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "PARMAR • ${uiState.settings.language.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.lastAssistantReply,
                        style = MaterialTheme.typography.bodyLarge,
                        color = IceWhite
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isGu) "ઝડપી ફોન કંટ્રોલ્સ" else "QUICK PHONE CONTROLS",
                        style = MaterialTheme.typography.labelLarge,
                        color = CoolSlate
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Filled.FlashlightOn,
                            label = if (isGu) {
                                if (uiState.isFlashlightOn) "ટોર્ચ ચાલુ" else "ટોર્ચ બંધ"
                            } else {
                                if (uiState.isFlashlightOn) "Torch ON" else "Torch OFF"
                            },
                            active = uiState.isFlashlightOn,
                            testTag = "quick_torch_tile",
                            onClick = { onQuickAction(AssistantActionType.FLASHLIGHT_ON) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Filled.AccessTime,
                            label = if (isGu) "સમય વાંચો" else "Time & Date",
                            testTag = "quick_time_tile",
                            onClick = { onQuickAction(AssistantActionType.READ_DATE_TIME) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Filled.Wifi,
                            label = if (isGu) "વાઇફાઇ" else "Wi-Fi",
                            testTag = "quick_wifi_tile",
                            onClick = { onQuickAction(AssistantActionType.OPEN_WIFI_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Filled.Bluetooth,
                            label = if (isGu) "બ્લૂટૂથ" else "Bluetooth",
                            testTag = "quick_bt_tile",
                            onClick = { onQuickAction(AssistantActionType.OPEN_BLUETOOTH_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Filled.Lock,
                            label = if (isGu) "લોક" else "Lock",
                            testTag = "quick_lock_tile",
                            onClick = { onQuickAction(AssistantActionType.LOCK_SCREEN) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isGu) "PARMAR AI મોડ્યુલ્સ" else "ALL WORKSPACE MODULES",
                        style = MaterialTheme.typography.labelLarge,
                        color = CoolSlate
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Filled.GraphicEq,
                            label = if (isGu) "વોઇસ કન્સોલ" else "Voice Console",
                            testTag = "hub_voice_tile",
                            onClick = onNavigateToVoice,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Filled.Chat,
                            label = if (isGu) "AI ટેક્સ્ટ ચેટ" else "AI Text Chat",
                            testTag = "hub_chat_tile",
                            onClick = onNavigateToChat,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Filled.Description,
                            label = if (isGu) "નોટ્સ (${uiState.notes.size})" else "Notes (${uiState.notes.size})",
                            testTag = "hub_notes_tile",
                            onClick = onNavigateToNotes,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Filled.Alarm,
                            label = if (isGu) "રિમાઇન્ડર (${uiState.reminders.count { !it.isCompleted }})" else "Reminders (${uiState.reminders.count { !it.isCompleted }})",
                            testTag = "hub_reminders_tile",
                            onClick = onNavigateToReminders,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Filled.Memory,
                            label = if (isGu) "મેમરી (${uiState.memories.size})" else "Memory (${uiState.memories.size})",
                            testTag = "hub_memory_tile",
                            onClick = onNavigateToMemory,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Filled.Security,
                            label = if (isGu) "પરવાનગી અને સેટઅપ" else "Permissions",
                            testTag = "hub_permissions_tile",
                            onClick = onNavigateToPermissions,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
