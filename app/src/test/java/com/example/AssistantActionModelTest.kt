package com.example

import com.example.ai.AssistantActionType
import com.example.data.local.AssistantLanguage
import com.example.data.local.ParmarSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantActionModelTest {

    @Test
    fun `parses wire action strings safely with fallback to NONE`() {
        assertEquals(AssistantActionType.FLASHLIGHT_ON, AssistantActionType.fromWire("flashlight_on"))
        assertEquals(AssistantActionType.CREATE_NOTE, AssistantActionType.fromWire("CREATE_NOTE"))
        assertEquals(AssistantActionType.NONE, AssistantActionType.fromWire("UNKNOWN_ACTION"))
        assertEquals(AssistantActionType.NONE, AssistantActionType.fromWire(null))
    }

    @Test
    fun `default settings match PARMAR AI specification`() {
        val defaults = ParmarSettings()
        assertEquals("DIAMOND", defaults.wakeWord)
        assertEquals(AssistantLanguage.GUJARATI, defaults.language)
        assertEquals(1.08f, defaults.voicePitch, 0.001f)
        assertEquals(true, defaults.requireDeviceAuthForSensitive)
    }
}
