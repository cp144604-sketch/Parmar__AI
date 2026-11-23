package com.example.ai

import java.util.Locale

/**
 * Pure Kotlin wake-word and stop-word matcher supporting English & Gujarati transliterations.
 */
object WakeWordMatcher {

    private val defaultDiamondVariants = setOf(
        "diamond",
        "diamonds",
        "daimond",
        "dymond",
        "diamon",
        "ડાયમંડ",
        "ડાયમન્ડ",
        "દાયમંડ"
    )

    fun containsWakeWord(transcript: String, configuredWakeWord: String): Boolean {
        val normalized = transcript.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return false

        val target = configuredWakeWord.trim().lowercase(Locale.ROOT).ifEmpty { "diamond" }
        val tokens = normalized.split(Regex("[\\s,.!?]+")).filter { it.isNotBlank() }

        if (target == "diamond") {
            return tokens.any { it in defaultDiamondVariants } ||
                defaultDiamondVariants.any { normalized.contains(it) }
        }

        return tokens.any { it == target || levenshtein(it, target) <= 1 } ||
            normalized.contains(target)
    }

    fun stripWakeWord(transcript: String, configuredWakeWord: String): String {
        val target = configuredWakeWord.trim().lowercase(Locale.ROOT).ifEmpty { "diamond" }
        var cleaned = transcript.trim()
        val variants = if (target == "diamond") defaultDiamondVariants else setOf(target)
        for (variant in variants) {
            cleaned = cleaned.replace(Regex("(?i)\\b${Regex.escape(variant)}\\b"), "").trim()
            cleaned = cleaned.replace(variant, "", ignoreCase = true).trim()
        }
        return cleaned.trim(',', '.', '!', '?', ' ')
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) {
                    prev
                } else {
                    1 + minOf(prev, dp[j], dp[j - 1])
                }
                prev = temp
            }
        }
        return dp[b.length]
    }
}
