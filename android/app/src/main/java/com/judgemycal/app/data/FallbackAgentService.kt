package com.judgemycal.app.data

import com.judgemycal.app.domain.CalorieEstimator
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.PersonaEngine
import com.judgemycal.app.domain.SwapEngine
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.WorkoutBuilder
import com.judgemycal.app.domain.WorkoutSession

/**
 * The offline path: the on-device engines that were the hackathon MVP's primary
 * mode, demoted to network-failure fallback (spec §6). Everything it returns is
 * flagged `fromFallback` so the UI can badge it honestly.
 */
class FallbackAgentService : AgentService {

    override suspend fun estimateMeal(
        persona: Persona,
        imageJpeg: ByteArray?,
        note: String,
    ): MealEstimate {
        val seed = imageJpeg?.size?.toLong() ?: System.currentTimeMillis()
        val estimate = CalorieEstimator.estimate(seed = seed, note = note)
        return estimate.copy(personaReaction = PersonaEngine.react(persona, estimate))
    }

    override suspend fun suggestSwaps(persona: Persona, items: List<String>): SwapResult =
        SwapEngine.suggestSwaps(items)

    override suspend fun buildWorkout(
        persona: Persona,
        goal: String,
        minutes: Int,
        equipment: String,
    ): WorkoutSession = WorkoutBuilder.buildSession(goal, minutes, equipment)
}
