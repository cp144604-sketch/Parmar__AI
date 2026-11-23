package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AssistantLanguage
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.components.StatusBannerBar
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: ParmarUiState,
    onBack: () -> Unit,
    onSaveWakeWord: (String) -> Unit,
    onLanguageSelected: (AssistantLanguage) -> Unit,
    onVoiceSpeedChanged: (Float) -> Unit,
    onVoicePitchChanged: (Float) -> Unit,
    onFemaleVoiceSelected: (String) -> Unit,
    onTestFemaleVoice: () -> Unit,
    onAiCloudToggle: (Boolean) -> Unit,
    onOfflineModeToggle: (Boolean) -> Unit,
    onBackgroundListeningToggle: (Boolean) -> Unit,
    onVoiceProfileToggle: (Boolean, String) -> Unit,
    onRequireDeviceAuthToggle: (Boolean) -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onClearChatHistory: () -> Unit,
    onStopListening: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val s = uiState.settings
    var wakeWordInput by remember(s.wakeWord) { mutableStateOf(s.wakeWord) }
    var voiceProfileInput by remember(s.voiceProfileTag) { mutableStateOf(s.voiceProfileTag) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        ParmarTelemetryHeader(
            title = "Settings",
            voiceState = uiState.voiceState,
            wakeWord = s.wakeWord,
            isOnline = uiState.isNetworkConnected,
            isOfflineModeEnabled = s.offlineModeEnabled,
            onStopListening = onStopListening,
            trailingActions = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IceWhite)
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
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Wake Word
            item {
                ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("WAKE & STOP WORD", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wakeWordInput,
                            onValueChange = { wakeWordInput = it },
                            label = { Text("Wake Word (Default: DIAMOND)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                focusedTextColor = IceWhite,
                                unfocusedTextColor = IceWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wake_word_input")
                        )
                        Button(
                            onClick = { onSaveWakeWord(wakeWordInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue)
                        ) { Text("Apply") }
                    }
                }
            }

            // 2. Language
            item {
                ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("ASSISTANT LANGUAGE", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AssistantLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = s.language == lang,
                                onClick = { onLanguageSelected(lang) },
                                label = { Text(lang.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalElectricBlue,
                                    selectedLabelColor = IceWhite
                                )
                            )
                        }
                    }
                }
            }

            // 3. Female Voice Settings & Speed & Pitch
            item {
                ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FEMALE VOICE & SPEECH", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        OutlinedButton(onClick = onTestFemaleVoice, modifier = Modifier.testTag("test_voice_btn")) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Voice", color = IceWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Voice Speed: ${String.format("%.2fx", s.voiceSpeed)}", color = IceWhite, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = s.voiceSpeed,
                        onValueChange = onVoiceSpeedChanged,
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = RoyalElectricBlue)
                    )

                    Text("Female Voice Pitch: ${String.format("%.2f", s.voicePitch)}", color = IceWhite, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = s.voicePitch,
                        onValueChange = onVoicePitchChanged,
                        valueRange = 0.8f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = RoyalElectricBlue)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assistant Volume: ${uiState.volumePercent}%", color = IceWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = uiState.volumePercent.toFloat(),
                        onValueChange = { onVolumeChanged(it.roundToInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = RoyalElectricBlue)
                    )

                    if (uiState.availableVoices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Device Female Voices (${uiState.availableVoices.size}):", style = MaterialTheme.typography.labelMedium, color = CoolSlate)
                        uiState.availableVoices.take(5).forEach { voice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFemaleVoiceSelected(voice.name) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = s.selectedFemaleVoiceName == voice.name,
                                    onClick = { onFemaleVoiceSelected(voice.name) }
                                )
                                Text(
                                    text = voice.displayLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IceWhite
                                )
                            }
                        }
                    }
                }
            }

            // 4. AI Service & Offline Mode & Background Listening
            item {
                ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("AI BRAIN & EXECUTION MODES", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(8.dp))

                    val aiStatusText = if (uiState.isNetworkConnected && s.aiCloudEnabled && !s.offlineModeEnabled) {
                        "Connected • Firebase AI Logic (Gemini 3.8 Flash + App Check)"
                    } else {
                        "Offline Mode • Local Commands, Notes & Reminders Only"
                    }
                    Text(
                        text = aiStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.isNetworkConnected && !s.offlineModeEnabled) EmeraldActive else AmberWarning
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleRow(
                        title = "Cloud AI Service (Gemini)",
                        subtitle = "Natural Gujarati & English Q&A when internet is available",
                        checked = s.aiCloudEnabled,
                        onCheckedChange = onAiCloudToggle,
                        testTag = "toggle_ai_cloud"
                    )
                    SettingsToggleRow(
                        title = "Force Offline Mode",
                        subtitle = "Restricts PARMAR strictly to offline local commands & notes",
                        checked = s.offlineModeEnabled,
                        onCheckedChange = onOfflineModeToggle,
                        testTag = "toggle_offline_mode"
                    )
                    SettingsToggleRow(
                        title = "Background Listening Service",
                        subtitle = "Runs Android Foreground Microphone Service with persistent notification for \"${s.wakeWord}\"",
                        checked = s.backgroundListeningEnabled,
                        onCheckedChange = onBackgroundListeningToggle,
                        testTag = "toggle_background_listening"
                    )
                }
            }

            // 5. Optional Voice Profile & Privacy Controls
            item {
                ParmarGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = GlassBorder) {
                    Text("VOICE PROFILE & PRIVACY CONTROLS", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsToggleRow(
                        title = "Optional Voice Profile Personalization",
                        subtitle = "Personalizes greeting only. Never treated as security authentication.",
                        checked = s.voiceProfileEnabled,
                        onCheckedChange = { onVoiceProfileToggle(it, voiceProfileInput) },
                        testTag = "toggle_voice_profile"
                    )
                    if (s.voiceProfileEnabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = voiceProfileInput,
                                onValueChange = { voiceProfileInput = it },
                                label = { Text("Profile Label") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    focusedTextColor = IceWhite,
                                    unfocusedTextColor = IceWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { onVoiceProfileToggle(true, voiceProfileInput) }) {
                                Text("Save")
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = "Device Auth for Private Memory",
                        subtitle = "Requires fingerprint, PIN, or pattern to reveal private entries",
                        checked = s.requireDeviceAuthForSensitive,
                        onCheckedChange = onRequireDeviceAuthToggle,
                        testTag = "toggle_require_auth"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onClearChatHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Local Conversation History", color = CrimsonAlert)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = IceWhite)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
