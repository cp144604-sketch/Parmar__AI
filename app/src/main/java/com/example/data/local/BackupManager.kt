package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val notes: List<NoteEntity>,
    val reminders: List<ReminderEntity>,
    val memories: List<MemoryEntity>
)

object BackupManager {

    fun exportToJson(
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        memories: List<MemoryEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "PARMAR_AI")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val notesArr = JSONArray()
        notes.forEach { note ->
            notesArr.put(JSONObject().apply {
                put("title", note.title)
                put("content", note.content)
                put("isPinned", note.isPinned)
                put("updatedAt", note.updatedAt)
            })
        }
        root.put("notes", notesArr)

        val remindersArr = JSONArray()
        reminders.forEach { reminder ->
            remindersArr.put(JSONObject().apply {
                put("title", reminder.title)
                put("triggerAtMillis", reminder.triggerAtMillis)
                put("isCompleted", reminder.isCompleted)
                put("createdAt", reminder.createdAt)
            })
        }
        root.put("reminders", remindersArr)

        val memoriesArr = JSONArray()
        memories.forEach { memory ->
            memoriesArr.put(JSONObject().apply {
                put("keyLabel", memory.keyLabel)
                put("valueText", memory.valueText)
                put("category", memory.category)
                put("isPrivate", memory.isPrivate)
                put("approvedByUser", memory.approvedByUser)
                put("createdAt", memory.createdAt)
            })
        }
        root.put("memories", memoriesArr)

        return root.toString(2)
    }

    fun parseFromJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson)
        val notes = mutableListOf<NoteEntity>()
        val notesArr = root.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until notesArr.length()) {
            val obj = notesArr.getJSONObject(i)
            notes.add(
                NoteEntity(
                    title = obj.optString("title", "Untitled"),
                    content = obj.optString("content", ""),
                    isPinned = obj.optBoolean("isPinned", false),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        val reminders = mutableListOf<ReminderEntity>()
        val remindersArr = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until remindersArr.length()) {
            val obj = remindersArr.getJSONObject(i)
            reminders.add(
                ReminderEntity(
                    title = obj.optString("title", "Reminder"),
                    triggerAtMillis = obj.optLong("triggerAtMillis", System.currentTimeMillis() + 3600_000L),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val memories = mutableListOf<MemoryEntity>()
        val memoriesArr = root.optJSONArray("memories") ?: JSONArray()
        for (i in 0 until memoriesArr.length()) {
            val obj = memoriesArr.getJSONObject(i)
            memories.add(
                MemoryEntity(
                    keyLabel = obj.optString("keyLabel", "Memory"),
                    valueText = obj.optString("valueText", ""),
                    category = obj.optString("category", "Preference"),
                    isPrivate = obj.optBoolean("isPrivate", false),
                    approvedByUser = obj.optBoolean("approvedByUser", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return BackupPayload(notes = notes, reminders = reminders, memories = memories)
    }
}
