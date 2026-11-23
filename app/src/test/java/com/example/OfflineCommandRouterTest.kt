package com.example

import com.example.ai.AssistantActionType
import com.example.ai.OfflineCommandRouter
import com.example.data.local.AssistantLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCommandRouterTest {

    @Test
    fun `routes Gujarati flashlight and task commands locally`() {
        val torch = OfflineCommandRouter.route(
            input = "ફ્લેશલાઇટ ચાલુ કરો",
            preferredLanguage = AssistantLanguage.GUJARATI,
            wakeWord = "DIAMOND",
            isOnline = false
        )
        assertNotNull(torch)
        assertEquals(AssistantActionType.FLASHLIGHT_ON, torch?.action)
        assertEquals("gu", torch?.language)

        val tasks = OfflineCommandRouter.route(
            input = "આજે શું કામ કરવાનું છે?",
            preferredLanguage = AssistantLanguage.GUJARATI,
            wakeWord = "DIAMOND",
            isOnline = true
        )
        assertNotNull(tasks)
        assertEquals(AssistantActionType.LIST_REMINDERS, tasks?.action)
    }

    @Test
    fun `routes stop word DIAMOND during active session`() {
        val stop = OfflineCommandRouter.route(
            input = "DIAMOND",
            preferredLanguage = AssistantLanguage.GUJARATI,
            wakeWord = "DIAMOND",
            isOnline = true
        )
        assertNotNull(stop)
        assertEquals(AssistantActionType.STOP_SESSION, stop?.action)
    }

    @Test
    fun `defers open-ended questions to Gemini when online and honestly reports offline when disconnected`() {
        val onlineOpen = OfflineCommandRouter.route(
            input = "Explain quantum computing in simple words",
            preferredLanguage = AssistantLanguage.ENGLISH,
            wakeWord = "DIAMOND",
            isOnline = true
        )
        assertNull(onlineOpen)

        val offlineOpen = OfflineCommandRouter.route(
            input = "Explain quantum computing in simple words",
            preferredLanguage = AssistantLanguage.ENGLISH,
            wakeWord = "DIAMOND",
            isOnline = false
        )
        assertNotNull(offlineOpen)
        assertEquals(AssistantActionType.NONE, offlineOpen?.action)
        assertTrue(offlineOpen?.isOffline == true)
        assertTrue(offlineOpen?.reply?.contains("offline", ignoreCase = true) == true)
    }
}
