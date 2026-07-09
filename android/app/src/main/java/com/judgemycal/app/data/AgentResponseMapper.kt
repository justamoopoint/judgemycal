package com.judgemycal.app.data

import com.judgemycal.app.domain.Confidence
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.MealItem
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.SwapSuggestion
import com.judgemycal.app.domain.WorkoutSession
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Extracts structured capability results from an ADK /run event stream.
 *
 * The agent's tools return persona-neutral dicts (the exact structures in
 * `estimator.py`, `suggest_swaps`, `build_session`); those arrive as
 * functionResponse parts alongside the persona-voiced final text. Reading the
 * structure from the tool response — not by parsing prose — is what keeps the
 * confidence-band UI honest.
 */
object AgentResponseMapper {

    fun finalText(events: List<WireEvent>): String =
        events.asReversed().firstNotNullOfOrNull { event ->
            if (event.partial == true) return@firstNotNullOfOrNull null
            event.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.takeIf { it.isNotBlank() }
        } ?: ""

    fun functionResponse(events: List<WireEvent>, toolName: String): JsonObject? =
        events.asReversed().firstNotNullOfOrNull { event ->
            event.content?.parts?.firstNotNullOfOrNull { part ->
                part.functionResponse?.takeIf { it.name == toolName }?.response
            }
        }

    fun mealEstimate(response: JsonObject, personaReaction: String): MealEstimate {
        val items = response["items"]?.jsonArray?.map { el ->
            val o = el.jsonObject
            MealItem(
                name = o.str("name"),
                grams = o.int("grams"),
                kcal = o.int("kcal"),
                kcalLow = o.int("kcal_low"),
                kcalHigh = o.int("kcal_high"),
                protein = o.dbl("protein"),
                carbs = o.dbl("carbs"),
                fat = o.dbl("fat"),
                confidence = confidence(o.str("confidence")),
            )
        }.orEmpty()
        return MealEstimate(
            items = items,
            totalKcal = response.int("total_kcal"),
            totalLow = response.int("total_low"),
            totalHigh = response.int("total_high"),
            protein = response.dbl("protein"),
            carbs = response.dbl("carbs"),
            fat = response.dbl("fat"),
            overallConfidence = confidence(response.str("overall_confidence")),
            lowestConfidenceItem = response["lowest_confidence_item"]
                ?.jsonPrimitive?.contentOrNull,
            note = response.str("note"),
            fromFallback = false,
            personaReaction = personaReaction,
        )
    }

    fun swapResult(response: JsonObject, personaReaction: String): SwapResult =
        SwapResult(
            swaps = response["swaps"]?.jsonArray?.map { el ->
                val o = el.jsonObject
                SwapSuggestion(item = o.str("item"), swap = o.str("swap"))
            }.orEmpty(),
            note = response.str("note"),
            fromFallback = false,
            personaReaction = personaReaction,
        )

    fun workoutSession(response: JsonObject, personaReaction: String): WorkoutSession {
        val structure = response["structure"]?.jsonObject
        return WorkoutSession(
            goal = response.str("goal"),
            totalMinutes = response.int("total_minutes"),
            warmupMin = structure.int("warmup_min"),
            mainMin = structure.int("main_min"),
            cooldownMin = structure.int("cooldown_min"),
            mainBlock = response["main_block"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            equipment = response.str("equipment"),
            note = response.str("note"),
            timestampMillis = System.currentTimeMillis(),
            fromFallback = false,
            personaReaction = personaReaction,
        )
    }

    private fun confidence(value: String): Confidence =
        runCatching { Confidence.valueOf(value.uppercase()) }.getOrDefault(Confidence.LOW)

    private fun JsonObject?.str(key: String): String =
        this?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject?.int(key: String): Int =
        this?.get(key)?.jsonPrimitive?.intOrNull ?: 0

    private fun JsonObject?.dbl(key: String): Double =
        this?.get(key)?.jsonPrimitive?.doubleOrNull ?: 0.0
}
