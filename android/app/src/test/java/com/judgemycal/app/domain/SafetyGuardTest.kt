package com.judgemycal.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGuardTest {

    private val distress = listOf(
        "I feel like I should just stop eating",
        "I've been starving myself all week",
        "haven't eaten since Tuesday",
        "I'm skipping meals to hit my goal",
        "I made myself sick after dinner",
        "I hate my body",
        "I'm so fat and worthless",
        "I want to disappear",
        "maybe I should punish myself with a workout",
    )

    private val benign = listOf(
        "what's in this meal?",
        "I had chicken and rice for lunch",
        "build me a 30 minute strength workout",
        "swap suggestions for my shopping basket please",
        "how many calories in a banana",
        "I ate a huge burger and fries today, oops",
        "no oil",
        "",
    )

    @Test
    fun `distress signals are detected`() {
        distress.forEach { assertTrue("should flag: $it", SafetyGuard.isDistress(it)) }
    }

    @Test
    fun `benign text is not flagged`() {
        benign.forEach { assertFalse("should NOT flag: $it", SafetyGuard.isDistress(it)) }
    }

    @Test
    fun `null is safe`() {
        assertFalse(SafetyGuard.isDistress(null))
    }

    @Test
    fun `punitive terms are neutralised`() {
        val out = SafetyGuard.governText("Don't be lazy, that plate is disgusting.")
        assertFalse(out.contains("lazy", ignoreCase = true))
        assertFalse(out.contains("disgusting", ignoreCase = true))
    }

    @Test
    fun `clean text passes through untouched`() {
        val text = "Nice plate! ~640 kcal (540–750). You logged it, that's the win."
        assertEquals(text, SafetyGuard.governText(text))
    }

    @Test
    fun `support message steps out of character`() {
        assertTrue(SafetyGuard.SUPPORT_MESSAGE.contains("step out of character"))
    }
}
