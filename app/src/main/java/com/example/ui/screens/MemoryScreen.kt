package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.MemoryEntity
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.components.StatusBannerBar
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(
    uiState: ParmarUiState,
    onSaveMemory: (Long, String, String, String, Boolean) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onUnlockPrivate: (Context) -> Unit,
    onLockPrivate: () -> Unit,
    onExportBackupJson: suspend () -> String,
    onImportBackupJson: (String) -> Unit,
    onStopListening: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = onExportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    Toast.makeText(context, "PARMAR backup exported successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Export failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { text ->
                if (!text.isNullOrBlank()) onImportBackupJson(text)
            }.onFailure { err ->
                Toast.makeText(context, "Restore failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParmarTelemetryHeader(
                title = "Approved Memory",
                voiceState = uiState.voiceState,
                wakeWord = uiState.settings.wakeWord,
                isOnline = uiState.isNetworkConnected,
                isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
                onStopListening = onStopListening,
                trailingActions = {
                    IconButton(
                        onClick = {
                            if (uiState.privateMemoryUnlocked) onLockPrivate()
                            else onUnlockPrivate(context)
                        },
                        modifier = Modifier.testTag("memory_lock_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.privateMemoryUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = if (uiState.privateMemoryUnlocked) "Lock private memories" else "Unlock private memories",
                            tint = if (uiState.privateMemoryUnlocked) ElectricCyan else AmberWarning
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
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "BACKUP & RESTORE (LOCAL JSON)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Export or restore all local Notes, Reminders, and Memories via Android Storage Access Framework.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { exportLauncher.launch("parmar_ai_backup.json") },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_btn")
                            ) {
                                Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup")
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_btn")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", color = IceWhite)
                            }
                        }
                    }
                }

                if (uiState.memories.isEmpty()) {
                    item {
                        ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            ) {
                                Icon(Icons.Filled.Memory, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(42.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No saved preferences yet, Sir", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                                Text(
                                    text = "PARMAR only remembers facts you approve.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate
                                )
                            }
                        }
                    }
                }

                items(uiState.memories, key = { it.id }) { mem ->
                    MemoryItemCard(
                        memory = mem,
                        privateUnlocked = uiState.privateMemoryUnlocked,
                        onUnlockRequest = { onUnlockPrivate(context) },
                        onDelete = { onDeleteMemory(mem.id) }
                    )
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
                .testTag("add_memory_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Memory")
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { key, value, category, isPrivate ->
                onSaveMemory(0L, key, value, category, isPrivate)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryEntity,
    privateUnlocked: Boolean,
    onUnlockRequest: () -> Unit,
    onDelete: () -> Unit
) {
    val hidden = memory.isPrivate && !privateUnlocked
    ParmarGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (memory.isPrivate) AmberWarning.copy(alpha = 0.6f) else GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${memory.category.uppercase()} • ${memory.keyLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (memory.isPrivate) AmberWarning else ElectricCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hidden) "•••••••• (Protected — Tap lock to verify)" else memory.valueText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IceWhite
                )
            }
            if (hidden) {
                IconButton(onClick = onUnlockRequest) {
                    Icon(Icons.Filled.Lock, contentDescription = "Authenticate to view", tint = AmberWarning)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete memory", tint = CrimsonAlert)
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Preference") }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepGraphite,
        title = { Text("Save Approved Memory", color = IceWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Label (e.g., Morning Tea)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Details") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Private (Require Device PIN/Biometric)", style = MaterialTheme.typography.bodyMedium, color = IceWhite, modifier = Modifier.weight(1f))
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(key, value, category, isPrivate) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue)
            ) { Text("Approve & Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CoolSlate) }
        }
    )
}
