package com.example.ai

import android.util.Log
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class ParmarAiBrain {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "PARMAR's spoken and displayed response. Address the user respectfully as Sir. Use natural Gujarati by default unless the user speaks or requests English."
            ),
            "language" to Schema.enumeration(
                values = listOf("gu", "en"),
                description = "Language code of the reply: 'gu' for Gujarati, 'en' for English."
            ),
            "action" to Schema.enumeration(
                values = AssistantActionType.entries.map { it.name },
                description = "Device or assistant action to execute alongside the reply, or NONE."
            ),
            "actionParam" to Schema.string(
                description = "Parameter for the action: app name for OPEN_APP; note text for CREATE_NOTE; reminder title for CREATE_REMINDER; HH:MM|label for SET_ALARM; key=value for PROPOSE_MEMORY; empty string otherwise."
            )
        ),
        optionalProperties = listOf("actionParam")
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@ParmarAiBrain.responseSchema
                temperature = 0.6f
            },
            systemInstruction = content {
                text(
                    """
                    You are PARMAR, a calm, intelligent, futuristic female personal AI voice assistant built for an Android smartphone (OPPO A3 5G).
                    CRITICAL PERSONALITY RULES:
                    1. Your name is PARMAR. Always refer to yourself as PARMAR.
                    2. Always address the user respectfully as "Sir".
                    3. Speak and respond in natural, warm, polite Gujarati (ગુજરાતી) by default. Switch to English ONLY when the user asks in English or explicitly requests English.
                    4. Keep voice-friendly responses concise, clear, and helpful (usually 1-3 sentences unless asked for detail).
                    5. Never claim to bypass Android security, PINs, passwords, biometrics, or hidden permissions.
                    6. When the user asks to remember a personal preference/fact, use action PROPOSE_MEMORY with actionParam formatted as "Key=Value" and ask Sir to confirm saving it.
                    7. When the user asks about today's tasks/schedule/notes, summarize the provided local notes and reminders context accurately!
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun processTurn(
        userMessage: String,
        preferredLanguage: AssistantLanguage,
        history: List<ChatMessageEntity>,
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        memories: List<MemoryEntity>,
        installedAppNames: List<String>
    ): Result<AiTurnOutput> {
        return try {
            val contextPrompt = buildPrompt(
                userMessage = userMessage,
                preferredLanguage = preferredLanguage,
                history = history,
                notes = notes,
                reminders = reminders,
                memories = memories,
                installedAppNames = installedAppNames
            )

            val response = withTimeout(45_000L) {
                model.generateContent(contextPrompt)
            }

            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(rawJson)
            val reply = json.optString("reply", "").ifBlank {
                if (preferredLanguage == AssistantLanguage.GUJARATI) {
                    "Sir, હું તમારી મદદ કરવા તૈયાર છું."
                } else {
                    "Sir, I am ready to help you."
                }
            }
            val lang = json.optString("language", preferredLanguage.code)
            val action = AssistantActionType.fromWire(json.optString("action"))
            val actionParam = json.optString("actionParam", "")

            Result.success(
                AiTurnOutput(
                    reply = reply,
                    language = lang,
                    action = action,
                    actionParam = actionParam,
                    isOffline = false
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out", e)
            Result.failure(Exception("AI response timed out after 45 seconds. Please check connection and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        userMessage: String,
        preferredLanguage: AssistantLanguage,
        history: List<ChatMessageEntity>,
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        memories: List<MemoryEntity>,
        installedAppNames: List<String>
    ): String {
        val publicMemories = memories.filter { !it.isPrivate && it.approvedByUser }
            .take(12)
            .joinToString("; ") { "${it.keyLabel}: ${it.valueText}" }
            .ifBlank { "None saved yet" }

        val activeReminders = reminders.filter { !it.isCompleted }
            .take(8)
            .joinToString("; ") { it.title }
            .ifBlank { "No pending reminders" }

        val recentNotes = notes.take(8)
            .joinToString("; ") { "${it.title}: ${it.content.take(80)}" }
            .ifBlank { "No notes saved" }

        val recentHistory = history.takeLast(8).joinToString("\n") { msg ->
            val speaker = if (msg.role == "user") "Sir" else "PARMAR"
            "$speaker: ${msg.text}"
        }

        val appsSummary = installedAppNames.take(30).joinToString(", ")

        return """
            Preferred Default Language: ${preferredLanguage.displayName} (${preferredLanguage.code})
            Approved Public Memory: $publicMemories
            Pending Local Reminders: $activeReminders
            Recent Local Notes: $recentNotes
            Sample Installed Apps: $appsSummary
            
            Recent Conversation:
            $recentHistory
            
            Sir's Current Request: $userMessage
        """.trimIndent()
    }

    companion object {
        private const val TAG = "ParmarAiBrain"
    }
}
