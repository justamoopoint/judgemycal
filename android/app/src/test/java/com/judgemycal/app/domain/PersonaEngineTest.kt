package com.judgemycal.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaEngineTest {

    private val estimate = MealEstimate(
        items = emptyList(),
        totalKcal = 640, totalLow = 540, totalHigh = 750,
        protein = 12.0, carbs = 80.0, fat = 20.0,
        overallConfidence = Confidence.LOW,
        lowestConfidenceItem = "white rice",
    )

    @Test
    fun `every persona keeps the range visible`() {
        Persona.entries.forEach { persona ->
            val text = PersonaEngine.react(persona, estimate)
            assertTrue("$persona dropped the range", text.contains("540") && text.contains("750"))
        }
    }

    @Test
    fun `uncertain estimates name the shaky item`() {
        Persona.entries.forEach { persona ->
            assertTrue(PersonaEngine.react(persona, estimate).contains("white rice"))
        }
    }

    @Test
    fun `no persona uses punitive terms`() {
        val banned = listOf("disgusting", "lazy", "failure", "greedy", "worthless")
        val confident = estimate.copy(
            overallConfidence = Confidence.HIGH,
            lowestConfidenceItem = null,
            protein = 40.0,
        )
        Persona.entries.forEach { persona ->
            listOf(estimate, confident).forEach { est ->
                val text = PersonaEngine.react(persona, est).lowercase()
                banned.forEach { term -> assertFalse("$persona said '$term'", term in text) }
            }
        }
    }

    @Test
    fun `buddy is the default persona`() {
        assertTrue(Persona.DEFAULT == Persona.BUDDY)
        assertTrue(Persona.fromKey(null) == Persona.BUDDY)
        assertTrue(Persona.fromKey("nonsense") == Persona.BUDDY)
    }
}
