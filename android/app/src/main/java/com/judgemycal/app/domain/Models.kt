package com.judgemycal.app.domain

import kotlinx.serialization.Serializable

enum class Confidence { HIGH, MEDIUM, LOW }

enum class Persona(val key: String) {
    BUDDY("buddy"), AUNTIE("auntie"), COACH("coach");

    companion object {
        val DEFAULT = BUDDY // the safe default; Auntie/Coach are opt-in
        fun fromKey(key: String?): Persona =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

@Serializable
data class MealItem(
    val name: String,
    val grams: Int,
    val kcal: Int,
    val kcalLow: Int,
    val kcalHigh: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val confidence: Confidence,
)

/** A persona-neutral estimate. Always a range — never a fake-precise number. */
@Serializable
data class MealEstimate(
    val items: List<MealItem>,
    val totalKcal: Int,
    val totalLow: Int,
    val totalHigh: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val overallConfidence: Confidence,
    val lowestConfidenceItem: String? = null,
    val note: String = "",
    /** True when produced by the on-device fallback engine instead of the backend. */
    val fromFallback: Boolean = false,
    /** Persona-voiced reaction (from the backend agent, or local PersonaEngine offline). */
    val personaReaction: String = "",
)

@Serializable
data class LoggedMeal(
    val name: String,
    val kcal: Int,
    val kcalLow: Int,
    val kcalHigh: Int,
    val timestampMillis: Long,
)

@Serializable
data class SwapSuggestion(val item: String, val swap: String)

@Serializable
data class SwapResult(
    val swaps: List<SwapSuggestion>,
    val note: String = "",
    val fromFallback: Boolean = false,
    val personaReaction: String = "",
)

@Serializable
data class WorkoutSession(
    val goal: String,
    val totalMinutes: Int,
    val warmupMin: Int,
    val mainMin: Int,
    val cooldownMin: Int,
    val mainBlock: List<String>,
    val equipment: String,
    val note: String = "",
    val timestampMillis: Long = 0L,
    val fromFallback: Boolean = false,
    val personaReaction: String = "",
)
