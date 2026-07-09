package com.judgemycal.app.data

import com.judgemycal.app.domain.Confidence
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the client to the ADK /run wire shape: a realistic event stream with a
 * functionResponse (the structured tool result) followed by the persona-voiced
 * final text. If ADK's event JSON drifts on upgrade, this fails first.
 */
class AgentResponseMapperTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val runEventsJson = """
    [
      {
        "id": "e1", "author": "cv_calorie", "invocationId": "i1",
        "content": {
          "role": "model",
          "parts": [
            {"functionCall": {"id": "fc1", "name": "estimate_meal", "args": {"image_path": "m.jpg"}}}
          ]
        }
      },
      {
        "id": "e2", "author": "cv_calorie",
        "content": {
          "role": "user",
          "parts": [
            {"functionResponse": {"id": "fc1", "name": "estimate_meal", "response": {
              "items": [
                {"name": "grilled chicken thigh", "grams": 140, "kcal": 293,
                 "kcal_low": 264, "kcal_high": 322, "protein": 36.4, "carbs": 0.0,
                 "fat": 15.4, "confidence": "HIGH"},
                {"name": "white rice", "grams": 180, "kcal": 234,
                 "kcal_low": 152, "kcal_high": 316, "protein": 4.9, "carbs": 50.4,
                 "fat": 0.5, "confidence": "LOW"}
              ],
              "total_kcal": 527, "total_low": 416, "total_high": 638,
              "protein": 41.3, "carbs": 50.4, "fat": 15.9,
              "overall_confidence": "LOW",
              "lowest_confidence_item": "white rice",
              "note": ""
            }}}
          ]
        }
      },
      {
        "id": "e3", "author": "judgemycal", "turnComplete": true,
        "content": {
          "role": "model",
          "parts": [{"text": "Nice plate! ~527 kcal (416–638). You logged it, that's the win!"}]
        }
      }
    ]
    """.trimIndent()

    @Test
    fun `parses events, extracts structured estimate and final text`() {
        val events = json.decodeFromString<List<WireEvent>>(runEventsJson)

        val structured = AgentResponseMapper.functionResponse(events, "estimate_meal")
        assertNotNull(structured)

        val text = AgentResponseMapper.finalText(events)
        assertTrue(text.contains("416"))

        val estimate = AgentResponseMapper.mealEstimate(structured!!, text)
        assertEquals(527, estimate.totalKcal)
        assertEquals(416, estimate.totalLow)
        assertEquals(638, estimate.totalHigh)
        assertEquals(Confidence.LOW, estimate.overallConfidence)
        assertEquals("white rice", estimate.lowestConfidenceItem)
        assertEquals(2, estimate.items.size)
        assertEquals(Confidence.HIGH, estimate.items[0].confidence)
        assertFalse(estimate.fromFallback)
        assertTrue(estimate.personaReaction.contains("416"))
    }

    @Test
    fun `missing tool result returns null instead of inventing numbers`() {
        val events = json.decodeFromString<List<WireEvent>>(runEventsJson)
        assertEquals(null, AgentResponseMapper.functionResponse(events, "build_session"))
    }

    @Test
    fun `swap and workout responses parse`() {
        val swapJson = """
          {"swaps": [{"item": "fries", "swap": "roasted potato wedges"}],
           "note": "Suggestions only"}
        """.trimIndent()
        val swaps = AgentResponseMapper.swapResult(
            json.decodeFromString(swapJson), "your call!",
        )
        assertEquals(1, swaps.swaps.size)
        assertEquals("fries", swaps.swaps[0].item)

        val workoutJson = """
          {"goal": "strength", "total_minutes": 30,
           "structure": {"warmup_min": 5, "main_min": 21, "cooldown_min": 4},
           "main_block": ["squats", "push-ups"], "equipment": "none",
           "note": "Rest is valid."}
        """.trimIndent()
        val workout = AgentResponseMapper.workoutSession(
            json.decodeFromString(workoutJson), "let's go",
        )
        assertEquals(30, workout.totalMinutes)
        assertEquals(5, workout.warmupMin)
        assertEquals(listOf("squats", "push-ups"), workout.mainBlock)
    }
}
