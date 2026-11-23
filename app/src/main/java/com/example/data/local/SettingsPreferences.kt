package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.parmarDataStore: DataStore<Preferences> by preferencesDataStore(name = "parmar_settings")

enum class AssistantLanguage(val code: String, val localeTag: String, val displayName: String) {
    GUJARATI("gu", "gu-IN", "ગુજરાતી (Gujarati)"),
    ENGLISH("en", "en-IN", "English")
}

data class ParmarSettings(
    val wakeWord: String = "DIAMOND",
    val language: AssistantLanguage = AssistantLanguage.GUJARATI,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.08f,
    val selectedFemaleVoiceName: String = "",
    val offlineModeEnabled: Boolean = false,
    val backgroundListeningEnabled: Boolean = false,
    val aiCloudEnabled: Boolean = true,
    val voiceProfileEnabled: Boolean = false,
    val voiceProfileTag: String = "Sir (Primary Profile)",
    val requireDeviceAuthForSensitive: Boolean = true
)

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val WAKE_WORD = stringPreferencesKey("wake_word")
        val LANGUAGE = stringPreferencesKey("language")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val FEMALE_VOICE_NAME = stringPreferencesKey("female_voice_name")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val BACKGROUND_LISTENING = booleanPreferencesKey("background_listening")
        val AI_CLOUD_ENABLED = booleanPreferencesKey("ai_cloud_enabled")
        val VOICE_PROFILE_ENABLED = booleanPreferencesKey("voice_profile_enabled")
        val VOICE_PROFILE_TAG = stringPreferencesKey("voice_profile_tag")
        val REQUIRE_DEVICE_AUTH = booleanPreferencesKey("require_device_auth")
    }

    val settingsFlow: Flow<ParmarSettings> = context.parmarDataStore.data.map { prefs ->
        val langCode = prefs[Keys.LANGUAGE] ?: AssistantLanguage.GUJARATI.code
        val lang = if (langCode == AssistantLanguage.ENGLISH.code) {
            AssistantLanguage.ENGLISH
        } else {
            AssistantLanguage.GUJARATI
        }
        ParmarSettings(
            wakeWord = prefs[Keys.WAKE_WORD]?.takeIf { it.isNotBlank() } ?: "DIAMOND",
            language = lang,
            voiceSpeed = (prefs[Keys.VOICE_SPEED] ?: 1.0f).coerceIn(0.6f, 1.6f),
            voicePitch = (prefs[Keys.VOICE_PITCH] ?: 1.08f).coerceIn(0.8f, 1.4f),
            selectedFemaleVoiceName = prefs[Keys.FEMALE_VOICE_NAME] ?: "",
            offlineModeEnabled = prefs[Keys.OFFLINE_MODE] ?: false,
            backgroundListeningEnabled = prefs[Keys.BACKGROUND_LISTENING] ?: false,
            aiCloudEnabled = prefs[Keys.AI_CLOUD_ENABLED] ?: true,
            voiceProfileEnabled = prefs[Keys.VOICE_PROFILE_ENABLED] ?: false,
            voiceProfileTag = prefs[Keys.VOICE_PROFILE_TAG] ?: "Sir (Primary Profile)",
            requireDeviceAuthForSensitive = prefs[Keys.REQUIRE_DEVICE_AUTH] ?: true
        )
    }

    suspend fun setWakeWord(word: String) {
        val sanitized = word.trim().uppercase().ifBlank { "DIAMOND" }
        context.parmarDataStore.edit { it[Keys.WAKE_WORD] = sanitized }
    }

    suspend fun setLanguage(language: AssistantLanguage) {
        context.parmarDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.parmarDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.parmarDataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.8f, 1.4f) }
    }

    suspend fun setSelectedFemaleVoice(voiceName: String) {
        context.parmarDataStore.edit { it[Keys.FEMALE_VOICE_NAME] = voiceName }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.parmarDataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setBackgroundListening(enabled: Boolean) {
        context.parmarDataStore.edit { it[Keys.BACKGROUND_LISTENING] = enabled }
    }

    suspend fun setAiCloudEnabled(enabled: Boolean) {
        context.parmarDataStore.edit { it[Keys.AI_CLOUD_ENABLED] = enabled }
    }

    suspend fun setVoiceProfile(enabled: Boolean, tag: String) {
        context.parmarDataStore.edit {
            it[Keys.VOICE_PROFILE_ENABLED] = enabled
            it[Keys.VOICE_PROFILE_TAG] = tag.ifBlank { "Sir (Primary Profile)" }
        }
    }

    suspend fun setRequireDeviceAuth(enabled: Boolean) {
        context.parmarDataStore.edit { it[Keys.REQUIRE_DEVICE_AUTH] = enabled }
    }
}
