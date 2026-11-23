package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.AssistantLanguage
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarGlowingOrb
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.components.StatusBannerBar
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.DeepGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantScreen(
    uiState: ParmarUiState,
    onOrbClick: () -> Unit,
    onStartWakeStandby: () -> Unit,
    onStopAll: () -> Unit,
    onToggleLanguage: (AssistantLanguage) -> Unit,
    onRunSampleVoiceCommand: (String) -> Unit,
    onDismissBanner: () -> Unit
) {
    val isGu = uiState.settings.language == AssistantLanguage.GUJARATI
    val samples = if (isGu) {
        listOf(
            "આજે શું કામ કરવાનું છે?",
            "સમય જણાવો",
            "ફ્લેશલાઇટ ચાલુ કરો",
            "વોટ્સએપ ખોલો",
            "નોટ બુક્સ માટે મીટિંગ લખો",
            "બ્લૂટૂથ સેટિંગ્સ ખોલો"
        )
    } else {
        listOf(
            "What tasks do I have today?",
            "What time is it?",
            "Turn on flashlight",
            "Open YouTube",
            "Remind me call client at 5 PM",
            "Open Wi-Fi settings"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VoidBlack, DeepCobalt.copy(alpha = 0.30f), VoidBlack)
                )
            )
    ) {
        ParmarTelemetryHeader(
            title = "Voice Assistant",
            voiceState = uiState.voiceState,
            wakeWord = uiState.settings.wakeWord,
            isOnline = uiState.isNetworkConnected,
            isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
            onStopListening = onStopAll
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, tint = ElectricCyan)
                    AssistantLanguage.entries.forEach { lang ->
                        val selected = uiState.settings.language == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleLanguage(lang) },
                            label = { Text(lang.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalElectricBlue,
                                selectedLabelColor = IceWhite
                            )
                        )
                    }
                }
            }

            item {
                ParmarGlowingOrb(
                    voiceState = uiState.voiceState,
                    rmsLevel = uiState.rmsLevel,
                    isOfflineMode = !uiState.isNetworkConnected || uiState.settings.offlineModeEnabled,
                    orbSize = 248.dp,
                    onClick = onOrbClick
                )
            }

            item {
                ParmarGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricCyan.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (uiState.partialTranscript.isNotBlank()) "SIR SAID" else "PARMAR LIVE TRANSCRIPT",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.partialTranscript.ifBlank { uiState.lastAssistantReply },
                        style = MaterialTheme.typography.bodyLarge,
                        color = IceWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Wake & Stop Word: \"${uiState.settings.wakeWord}\" • Female TTS Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoolSlate
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onStartWakeStandby,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_wake_standby_btn")
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Listen \"${uiState.settings.wakeWord}\"")
                    }

                    OutlinedButton(
                        onClick = onStopAll,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_stop_btn")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, tint = CrimsonAlert)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Session", color = IceWhite)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isGu) "નમૂના વોઇસ કમાન્ડ્સ (ટેપ કરીને ચલાવો)" else "TAP TO TEST VOICE COMMANDS",
                        style = MaterialTheme.typography.labelLarge,
                        color = CoolSlate,
                        textAlign = TextAlign.Start
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        samples.forEach { cmd ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DeepGraphite,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onRunSampleVoiceCommand(cmd) }
                            ) {
                                Text(
                                    text = cmd,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IceWhite,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
