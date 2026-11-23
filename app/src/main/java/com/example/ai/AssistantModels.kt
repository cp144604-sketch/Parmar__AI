package com.example.ai

enum class AssistantActionType {
    NONE,
    OPEN_APP,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    READ_DATE_TIME,
    SET_ALARM,
    CREATE_NOTE,
    LIST_NOTES,
    CREATE_REMINDER,
    LIST_REMINDERS,
    PROPOSE_MEMORY,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_WIFI_SETTINGS,
    LOCK_SCREEN,
    STOP_SESSION;

    companion object {
        fun fromWire(value: String?): AssistantActionType {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: NONE
        }
    }
}

data class AiTurnOutput(
    val reply: String,
    val language: String,
    val action: AssistantActionType,
    val actionParam: String = "",
    val isOffline: Boolean = false
)

data class PendingMemoryProposal(
    val keyLabel: String,
    val valueText: String,
    val category: String = "Preference"
)
