package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedGraphite
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import com.example.voice.AssistantVoiceState

@Composable
fun ParmarGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        color = DeepGraphite.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ParmarTelemetryHeader(
    title: String,
    voiceState: AssistantVoiceState,
    wakeWord: String,
    isOnline: Boolean,
    isOfflineModeEnabled: Boolean,
    onStopListening: () -> Unit,
    trailingActions: @Composable () -> Unit = {}
) {
    val effectivelyOffline = !isOnline || isOfflineModeEnabled
    val (statusLabel, statusColor) = when (voiceState) {
        AssistantVoiceState.WAKE_WORD_STANDBY -> "STANDBY • $wakeWord" to RoyalElectricBlue
        AssistantVoiceState.ACTIVE_LISTENING -> "MIC ACTIVE" to ElectricCyan
        AssistantVoiceState.PROCESSING -> "THINKING" to ElectricCyan
        AssistantVoiceState.SPEAKING -> "PARMAR SPEAKING" to EmeraldActive
        AssistantVoiceState.STOPPED -> "STOPPED" to AmberWarning
        AssistantVoiceState.IDLE -> if (effectivelyOffline) "OFFLINE READY" to AmberWarning else "READY" to ElectricCyan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(VoidBlack, ObsidianSurface.copy(alpha = 0.92f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = IceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (effectivelyOffline) AmberWarning.copy(alpha = 0.16f) else ElevatedGraphite,
                    border = BorderStroke(
                        1.dp,
                        if (effectivelyOffline) AmberWarning.copy(alpha = 0.6f) else GlassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (effectivelyOffline) Icons.Filled.CloudOff else Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = if (effectivelyOffline) AmberWarning else ElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (effectivelyOffline) "OFFLINE" else "ONLINE",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (effectivelyOffline) AmberWarning else ElectricCyan
                        )
                    }
                }

                if (voiceState == AssistantVoiceState.WAKE_WORD_STANDBY ||
                    voiceState == AssistantVoiceState.ACTIVE_LISTENING
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CrimsonAlert.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onStopListening)
                            .testTag("stop_listening_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Stop listening",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STOP",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrimsonAlert
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            trailingActions()
        }
    }
}

@Composable
fun StatusBannerBar(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    val activeText = errorMessage ?: statusMessage ?: return
    val isError = errorMessage != null
    val accent = if (isError) CrimsonAlert else ElectricCyan

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeText,
                style = MaterialTheme.typography.bodyMedium,
                color = IceWhite,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss notification",
                    tint = IceWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (active) ElectricCyan else GlassBorder
    val bg = if (active) RoyalElectricBlue.copy(alpha = 0.28f) else DeepGraphite
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) ElectricCyan else IceWhite,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = IceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
