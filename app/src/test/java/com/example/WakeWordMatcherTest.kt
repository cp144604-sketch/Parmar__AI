package com.example

import com.example.ai.WakeWordMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordMatcherTest {

    @Test
    fun `detects DIAMOND wake word in English and Gujarati`() {
        assertTrue(WakeWordMatcher.containsWakeWord("Diamond", "DIAMOND"))
        assertTrue(WakeWordMatcher.containsWakeWord("hey diamond open whatsapp", "DIAMOND"))
        assertTrue(WakeWordMatcher.containsWakeWord("ડાયમંડ", "DIAMOND"))
        assertFalse(WakeWordMatcher.containsWakeWord("hello parmar", "DIAMOND"))
    }

    @Test
    fun `strips wake word cleanly to isolate inline command`() {
        assertEquals("", WakeWordMatcher.stripWakeWord("DIAMOND", "DIAMOND"))
        assertEquals("open youtube", WakeWordMatcher.stripWakeWord("diamond open youtube", "DIAMOND"))
    }

    @Test
    fun `supports custom configured wake word`() {
        assertTrue(WakeWordMatcher.containsWakeWord("Jarvis check time", "JARVIS"))
        assertFalse(WakeWordMatcher.containsWakeWord("hello world", "JARVIS"))
    }
}
