package com.example.data.repository

import com.example.data.local.BackupManager
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderDao
import com.example.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow

class ParmarRepository(
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
    private val memoryDao: MemoryDao,
    private val chatDao: ChatDao
) {
    val notesFlow: Flow<List<NoteEntity>> = noteDao.observeAll()
    val remindersFlow: Flow<List<ReminderEntity>> = reminderDao.observeAll()
    val memoriesFlow: Flow<List<MemoryEntity>> = memoryDao.observeAll()
    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = chatDao.observeAll()

    suspend fun saveNote(note: NoteEntity): Long = noteDao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteNote(id: Long) = noteDao.deleteById(id)
    suspend fun getNotesSnapshot(): List<NoteEntity> = noteDao.getAllSnapshot()

    suspend fun saveReminder(reminder: ReminderEntity): Long = reminderDao.upsert(reminder)
    suspend fun setReminderCompleted(id: Long, completed: Boolean) = reminderDao.setCompleted(id, completed)
    suspend fun deleteReminder(id: Long) = reminderDao.deleteById(id)
    suspend fun getRemindersSnapshot(): List<ReminderEntity> = reminderDao.getAllSnapshot()

    suspend fun saveMemory(memory: MemoryEntity): Long = memoryDao.upsert(memory)
    suspend fun deleteMemory(id: Long) = memoryDao.deleteById(id)
    suspend fun getMemoriesSnapshot(): List<MemoryEntity> = memoryDao.getAllSnapshot()

    suspend fun addChatMessage(message: ChatMessageEntity): Long = chatDao.insert(message)
    suspend fun getRecentChatHistory(limit: Int = 14): List<ChatMessageEntity> =
        chatDao.getRecent(limit).reversed()
    suspend fun clearChatHistory() = chatDao.clearAll()

    suspend fun exportBackupJson(): String {
        val notes = noteDao.getAllSnapshot()
        val reminders = reminderDao.getAllSnapshot()
        val memories = memoryDao.getAllSnapshot()
        return BackupManager.exportToJson(notes, reminders, memories)
    }

    suspend fun restoreFromBackupJson(rawJson: String): Triple<Int, Int, Int> {
        val payload = BackupManager.parseFromJson(rawJson)
        payload.notes.forEach { noteDao.upsert(it) }
        payload.reminders.forEach { reminderDao.upsert(it) }
        payload.memories.forEach { memoryDao.upsert(it) }
        return Triple(payload.notes.size, payload.reminders.size, payload.memories.size)
    }
}
