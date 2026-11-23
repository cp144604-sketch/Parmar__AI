package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderEntity
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    uiState: ParmarUiState,
    onSaveReminder: (Long, String, Long) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onOpenSystemAlarm: () -> Unit,
    onStopListening: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParmarTelemetryHeader(
                title = "Reminders & Alarms",
                voiceState = uiState.voiceState,
                wakeWord = uiState.settings.wakeWord,
                isOnline = uiState.isNetworkConnected,
                isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
                onStopListening = onStopListening,
                trailingActions = {
                    OutlinedButton(
                        onClick = onOpenSystemAlarm,
                        modifier = Modifier.testTag("system_alarm_btn")
                    ) {
                        Icon(
                            Icons.Filled.Alarm,
                            contentDescription = "Set System Clock Alarm",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clock", color = IceWhite)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.reminders.isEmpty()) {
                    item {
                        ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No reminders scheduled, Sir",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IceWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap + to schedule an offline notification reminder or use Clock for a system alarm.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate
                                )
                            }
                        }
                    }
                }

                items(uiState.reminders, key = { it.id }) { item ->
                    val timeFmt = remember(item.triggerAtMillis) {
                        SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault())
                            .format(Date(item.triggerAtMillis))
                    }
                    ParmarGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (item.isCompleted) GlassBorder else ElectricCyan.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggleComplete(item) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EmeraldActive,
                                    uncheckedColor = ElectricCyan
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (item.isCompleted) CoolSlate else IceWhite
                                )
                                Text(
                                    text = timeFmt,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.isCompleted) CoolSlate else ElectricCyan
                                )
                            }
                            IconButton(onClick = { onDeleteReminder(item.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete reminder",
                                    tint = CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = RoyalElectricBlue,
            contentColor = IceWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_reminder_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, triggerAt ->
                onSaveReminder(0L, title, triggerAt)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableIntStateOf(1) } // 0=15m, 1=1h, 2=3h, 3=Tomorrow 9AM

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepGraphite,
        title = { Text("Schedule Local Reminder", color = IceWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title (e.g., Call Office)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("When to notify:", style = MaterialTheme.typography.labelMedium, color = CoolSlate)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("In 15 min", "In 1 hour", "In 3 hours", "Tomorrow 9 AM").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedPreset == idx,
                            onClick = { selectedPreset = idx },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val trigger = when (selectedPreset) {
                        0 -> now + 15 * 60_000L
                        1 -> now + 60 * 60_000L
                        2 -> now + 3 * 60 * 60_000L
                        else -> Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                    }
                    onSave(title, trigger)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue)
            ) { Text("Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CoolSlate) }
        }
    )
}
