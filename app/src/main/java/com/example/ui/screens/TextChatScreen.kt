package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.components.StatusBannerBar
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.DeepGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedGraphite
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack

@Composable
fun TextChatScreen(
    uiState: ParmarUiState,
    onSendMessage: (String) -> Unit,
    onVoiceMicTap: () -> Unit,
    onStopListening: () -> Unit,
    onApproveMemory: (Boolean) -> Unit,
    onDismissMemory: () -> Unit,
    onClearHistory: () -> Unit,
    onDismissBanner: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isGu = uiState.settings.language == AssistantLanguage.GUJARATI

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        ParmarTelemetryHeader(
            title = "PARMAR Chat",
            voiceState = uiState.voiceState,
            wakeWord = uiState.settings.wakeWord,
            isOnline = uiState.isNetworkConnected,
            isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
            onStopListening = onStopListening,
            trailingActions = {
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_chat_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear conversation history",
                        tint = CoolSlate
                    )
                }
            }
        )

        StatusBannerBar(
            statusMessage = uiState.statusBanner,
            errorMessage = uiState.errorBanner,
            onDismiss = onDismissBanner
        )

        uiState.pendingMemoryProposal?.let { proposal ->
            ParmarGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                borderColor = ElectricCyan
            ) {
                Text(
                    text = "Save to PARMAR Memory?",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${proposal.keyLabel}: ${proposal.valueText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IceWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApproveMemory(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue)
                    ) { Text("Save") }
                    OutlinedButton(onClick = { onApproveMemory(true) }) {
                        Text("Save Private", color = ElectricCyan)
                    }
                    OutlinedButton(onClick = onDismissMemory) {
                        Text("Dismiss", color = CoolSlate)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.chatMessages.isEmpty()) {
                item {
                    ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Hello Sir! હું PARMAR છું.",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isGu) {
                                "તમે મને ગુજરાતી અથવા English માં કોઈપણ પ્રશ્ન પૂછી શકો છો — અથવા ફોન કમાન્ડ (ફ્લેશલાઇટ, રિમાઇન્ડર, નોટ્સ, એપ ખોલો) આપી શકો છો."
                            } else {
                                "Ask me anything in Gujarati or English, or ask me to open apps, toggle flashlight, check time, or save notes and reminders."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate
                        )
                    }
                }
            }

            items(uiState.chatMessages, key = { it.id }) { msg ->
                ChatBubbleItem(msg)
            }

            if (uiState.isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isGu) "PARMAR વિચારી રહી છે..." else "PARMAR is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        Surface(
            color = DeepGraphite,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onVoiceMicTap,
                    modifier = Modifier.testTag("chat_mic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Speak to PARMAR",
                        tint = ElectricCyan
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isGu) "Sir, અહીં લખો..." else "Message PARMAR, Sir...",
                            color = CoolSlate
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendMessage(trimmed)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = ElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(msg: ChatMessageEntity) {
    val isUser = msg.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) DeepCobalt else ElevatedGraphite,
            border = BorderStroke(
                1.dp,
                if (isUser) RoyalElectricBlue else GlassBorder
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "SIR" else "PARMAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) ElectricCyan else IceWhite.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IceWhite
                )
                if (!isUser && (msg.actionExecuted != null || msg.isOfflineResponse)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        msg.actionExecuted?.let { action ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = action,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                            }
                        }
                        if (msg.isOfflineResponse) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
