package com.example.ai

import com.example.data.local.AssistantLanguage
import java.util.Locale

/**
 * Deterministic offline command parser for Gujarati and English commands.
 * Never claims full conversational AI offline; clearly communicates offline capability limits.
 */
object OfflineCommandRouter {

    fun route(
        input: String,
        preferredLanguage: AssistantLanguage,
        wakeWord: String,
        isOnline: Boolean
    ): AiTurnOutput? {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val useEnglish = detectEnglishRequest(lower, preferredLanguage)
        val langCode = if (useEnglish) "en" else "gu"

        // Stop word check
        if (WakeWordMatcher.containsWakeWord(trimmed, wakeWord) &&
            WakeWordMatcher.stripWakeWord(trimmed, wakeWord).isEmpty()
        ) {
            return AiTurnOutput(
                reply = if (useEnglish) "Stopping active session, Sir." else "બરાબર Sir, હું સત્ર બંધ કરું છું.",
                language = langCode,
                action = AssistantActionType.STOP_SESSION,
                isOffline = true
            )
        }

        // Flashlight ON / OFF
        if ((lower.contains("flashlight") || lower.contains("torch") || lower.contains("ફ્લેશ") || lower.contains("ટોર્ચ")) &&
            (lower.contains("on") || lower.contains("start") || lower.contains("ચાલુ") || lower.contains("ખોલ"))
        ) {
            return AiTurnOutput(
                reply = if (useEnglish) "Turning flashlight on, Sir." else "Sir, ફ્લેશલાઇટ ચાલુ કરું છું.",
                language = langCode,
                action = AssistantActionType.FLASHLIGHT_ON,
                isOffline = true
            )
        }

        if ((lower.contains("flashlight") || lower.contains("torch") || lower.contains("ફ્લેશ") || lower.contains("ટોર્ચ")) &&
            (lower.contains("off") || lower.contains("stop") || lower.contains("બંધ"))
        ) {
            return AiTurnOutput(
                reply = if (useEnglish) "Turning flashlight off, Sir." else "Sir, ફ્લેશલાઇટ બંધ કરું છું.",
                language = langCode,
                action = AssistantActionType.FLASHLIGHT_OFF,
                isOffline = true
            )
        }

        // Date & Time
        if (lower.contains("time") || lower.contains("date") || lower.contains("today") ||
            lower.contains("સમય") || lower.contains("વાગ્યા") || lower.contains("તારીખ")
        ) {
            return AiTurnOutput(
                reply = "",
                language = langCode,
                action = AssistantActionType.READ_DATE_TIME,
                isOffline = true
            )
        }

        // Volume controls
        if (lower.contains("volume") || lower.contains("અવાજ") || lower.contains("વોલ્યુમ")) {
            return when {
                lower.contains("mute") || lower.contains("મ્યુટ") || lower.contains("બંધ") -> AiTurnOutput(
                    reply = "",
                    language = langCode,
                    action = AssistantActionType.VOLUME_MUTE,
                    isOffline = true
                )
                lower.contains("down") || lower.contains("low") || lower.contains("decrease") ||
                    lower.contains("ઓછ") || lower.contains("ઘટાડ") -> AiTurnOutput(
                    reply = "",
                    language = langCode,
                    action = AssistantActionType.VOLUME_DOWN,
                    isOffline = true
                )
                else -> AiTurnOutput(
                    reply = "",
                    language = langCode,
                    action = AssistantActionType.VOLUME_UP,
                    isOffline = true
                )
            }
        }

        // Bluetooth & Wi-Fi settings
        if (lower.contains("bluetooth") || lower.contains("બ્લૂટૂથ")) {
            return AiTurnOutput(
                reply = "",
                language = langCode,
                action = AssistantActionType.OPEN_BLUETOOTH_SETTINGS,
                isOffline = true
            )
        }

        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("network") ||
            lower.contains("વાઇફાઇ") || lower.contains("નેટવર્ક") || lower.contains("ઇન્ટરનેટ સેટિંગ")
        ) {
            return AiTurnOutput(
                reply = "",
                language = langCode,
                action = AssistantActionType.OPEN_WIFI_SETTINGS,
                isOffline = true
            )
        }

        // Lock phone
        if ((lower.contains("lock") && (lower.contains("phone") || lower.contains("screen"))) ||
            lower.contains("ફોન લોક") || lower.contains("સ્ક્રીન લોક")
        ) {
            return AiTurnOutput(
                reply = "",
                language = langCode,
                action = AssistantActionType.LOCK_SCREEN,
                isOffline = true
            )
        }

        // Check tasks / reminders / notes
        if (lower.contains("આજે શું કામ") || lower.contains("what is my") ||
            lower.contains("show reminder") || lower.contains("list reminder") ||
            lower.contains("રિમાઇન્ડર બતાવ") || lower.contains("મારા કામ")
        ) {
            return AiTurnOutput(
                reply = if (useEnglish) "Sir, checking your local reminders and notes."
                else "Sir, હું તમારા રિમાઇન્ડર અને નોટ્સ તપાસી આપું છું.",
                language = langCode,
                action = AssistantActionType.LIST_REMINDERS,
                isOffline = true
            )
        }

        if (lower.contains("show note") || lower.contains("list note") || lower.contains("નોટ્સ બતાવ")) {
            return AiTurnOutput(
                reply = if (useEnglish) "Sir, reading your saved notes." else "Sir, તમારી સેવ કરેલી નોટ્સ તપાસું છું.",
                language = langCode,
                action = AssistantActionType.LIST_NOTES,
                isOffline = true
            )
        }

        // Create Note directly offline
        if (lower.startsWith("note ") || lower.startsWith("save note ") || lower.startsWith("નોટ ") || lower.startsWith("નોંધ ")) {
            val content = trimmed
                .replaceFirst(Regex("^(save note|note|નોટ|નોંધ)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (content.isNotEmpty()) {
                return AiTurnOutput(
                    reply = if (useEnglish) "Sir, I saved that note locally." else "Sir, મેં આ નોટ લોકલી સેવ કરી દીધી છે.",
                    language = langCode,
                    action = AssistantActionType.CREATE_NOTE,
                    actionParam = content,
                    isOffline = true
                )
            }
        }

        // Create Reminder directly offline
        if (lower.startsWith("remind me ") || lower.startsWith("reminder ") || lower.startsWith("રિમાઇન્ડર ")) {
            val title = trimmed
                .replaceFirst(Regex("^(remind me to|remind me|reminder|રિમાઇન્ડર)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (title.isNotEmpty()) {
                return AiTurnOutput(
                    reply = if (useEnglish) "Sir, I added a local reminder for '$title'."
                    else "Sir, મેં '$title' માટે લોકલ રિમાઇન્ડર સેટ કર્યું છે.",
                    language = langCode,
                    action = AssistantActionType.CREATE_REMINDER,
                    actionParam = title,
                    isOffline = true
                )
            }
        }

        // Open installed app
        if (lower.startsWith("open ") || lower.contains(" ખોલો") || lower.startsWith("launch ")) {
            val appName = trimmed
                .replaceFirst(Regex("^(open|launch)\\s+", RegexOption.IGNORE_CASE), "")
                .replace("ખોલો", "")
                .trim()
            if (appName.isNotEmpty()) {
                return AiTurnOutput(
                    reply = "",
                    language = langCode,
                    action = AssistantActionType.OPEN_APP,
                    actionParam = appName,
                    isOffline = true
                )
            }
        }

        // If online and not a direct instant hardware shortcut, let Gemini AI Brain handle it naturally!
        if (isOnline) return null

        // Offline honest fallback for general queries
        val offlineMessage = if (useEnglish) {
            "Sir, I am currently in Offline Mode (no internet connection and no local LLM installed). I can execute offline commands like time, flashlight, volume, opening apps, and managing local notes or reminders."
        } else {
            "Sir, હાલ ઓફલાઇન મોડ સક્રિય છે (ઇન્ટરનેટ ઉપલબ્ધ નથી અને લોકલ AI મોડલ ઇન્સ્ટોલ નથી). ઓફલાઇન મોડમાં હું સમય, ફ્લેશલાઇટ, વોલ્યુમ, એપ્સ ખોલવી, અને લોકલ નોટ્સ-રિમાઇન્ડર ચલાવી શકું છું."
        }
        return AiTurnOutput(
            reply = offlineMessage,
            language = langCode,
            action = AssistantActionType.NONE,
            isOffline = true
        )
    }

    private fun detectEnglishRequest(lower: String, preferred: AssistantLanguage): Boolean {
        if (lower.contains("in english") || lower.contains("speak english") || lower.contains("અંગ્રેજીમાં")) {
            return true
        }
        if (lower.contains("gujarati") || lower.contains("ગુજરાતીમાં")) {
            return false
        }
        return preferred == AssistantLanguage.ENGLISH
    }
}
