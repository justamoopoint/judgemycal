package com.judgemycal.app

import com.judgemycal.app.data.AgentService
import com.judgemycal.app.domain.Confidence
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.MealItem
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.SwapSuggestion
import com.judgemycal.app.domain.WorkoutSession
import java.util.concurrent.atomic.AtomicInteger

/** Deterministic agent double: instant answers, counts calls. */
class FakeAgentService : AgentService {

    val estimateCalls = AtomicInteger(0)
    val swapCalls = AtomicInteger(0)
    val workoutCalls = AtomicInteger(0)

    override suspend fun estimateMeal(
        persona: Persona,
        imageJpeg: ByteArray?,
        note: String,
    ): MealEstimate {
        estimateCalls.incrementAndGet()
        return MealEstimate(
            items = listOf(
                MealItem(
                    name = "grilled chicken thigh", grams = 140, kcal = 293,
                    kcalLow = 264, kcalHigh = 322, protein = 36.4, carbs = 0.0,
                    fat = 15.4, confidence = Confidence.HIGH,
                ),
                MealItem(
                    name = "white rice", grams = 180, kcal = 234,
                    kcalLow = 152, kcalHigh = 316, protein = 4.9, carbs = 50.4,
                    fat = 0.5, confidence = Confidence.LOW,
                ),
            ),
            totalKcal = 527, totalLow = 416, totalHigh = 638,
            protein = 41.3, carbs = 50.4, fat = 15.9,
            overallConfidence = Confidence.LOW,
            lowestConfidenceItem = "white rice",
            note = note,
            personaReaction = "Nice plate! ~527 kcal (416–638). You logged it, that's the real win!",
        )
    }

    override suspend fun suggestSwaps(persona: Persona, items: List<String>): SwapResult {
        swapCalls.incrementAndGet()
        return SwapResult(
            swaps = items.map { SwapSuggestion(it, "a goal-friendly alternative") },
            note = "Suggestions only — your call, no pressure.",
        )
    }

    override suspend fun buildWorkout(
        persona: Persona,
        goal: String,
        minutes: Int,
        equipment: String,
    ): WorkoutSession {
        workoutCalls.incrementAndGet()
        return WorkoutSession(
            goal = goal, totalMinutes = minutes, warmupMin = 5,
            mainMin = minutes - 9, cooldownMin = 4,
            mainBlock = listOf("squats", "push-ups"), equipment = equipment,
            note = "Rest is a valid choice.",
        )
    }
}
