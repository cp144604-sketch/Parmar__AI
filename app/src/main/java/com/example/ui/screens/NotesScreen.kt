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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.NoteEntity
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    uiState: ParmarUiState,
    onSaveNote: (Long, String, String, Boolean) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onStopListening: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val filteredNotes = remember(uiState.notes, searchQuery) {
        if (searchQuery.isBlank()) uiState.notes
        else uiState.notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParmarTelemetryHeader(
                title = "Local Notes",
                voiceState = uiState.voiceState,
                wakeWord = uiState.settings.wakeWord,
                isOnline = uiState.isNetworkConnected,
                isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
                onStopListening = onStopListening
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ElectricCyan)
                },
                placeholder = { Text("Search saved notes...", color = CoolSlate) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = IceWhite,
                    unfocusedTextColor = IceWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("notes_search_field")
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredNotes.isEmpty()) {
                    item {
                        ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No notes saved yet, Sir",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IceWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap + to add a note, or say \"નોટ લખો...\" to PARMAR.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(filteredNotes, key = { it.id }) { note ->
                    val dateFmt = remember(note.updatedAt) {
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(note.updatedAt))
                    }
                    ParmarGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (note.isPinned) ElectricCyan else GlassBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = IceWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = { onTogglePin(note) }) {
                                    Icon(
                                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Pin note",
                                        tint = if (note.isPinned) ElectricCyan else CoolSlate
                                    )
                                }
                                IconButton(onClick = {
                                    editingNote = note
                                    showEditor = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit note", tint = IceWhite)
                                }
                                IconButton(onClick = { onDeleteNote(note.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = CrimsonAlert)
                                }
                            }
                        }
                        if (note.content.isNotBlank()) {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoolSlate
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = "Updated $dateFmt • Offline Local",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingNote = null
                showEditor = true
            },
            containerColor = RoyalElectricBlue,
            contentColor = IceWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_note_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Note")
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            initial = editingNote,
            onDismiss = { showEditor = false },
            onSave = { title, content ->
                onSaveNote(editingNote?.id ?: 0L, title, content, editingNote?.isPinned ?: false)
                showEditor = false
            }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepGraphite,
        title = {
            Text(
                text = if (initial == null) "New Local Note" else "Edit Note",
                color = IceWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Gujarati / English)") },
                    minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, content) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CoolSlate) }
        }
    )
}
