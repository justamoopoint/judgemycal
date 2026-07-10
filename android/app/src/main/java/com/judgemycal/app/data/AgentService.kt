package com.judgemycal.app.data

import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.WorkoutSession

/**
 * The one seam between the UI and "where answers come from". Production wires
 * [FallbackAgentService] behind [BackendAgentService]; instrumented tests swap
 * in a fake via [com.judgemycal.app.AppGraph].
 */
interface AgentService {

    suspend fun estimateMeal(
        persona: Persona,
        imageJpeg: ByteArray?,
        note: String,
    ): MealEstimate

    suspend fun suggestSwaps(persona: Persona, items: List<String>): SwapResult

    suspend fun buildWorkout(
        persona: Persona,
        goal: String,
        minutes: Int,
        equipment: String,
    ): WorkoutSession
}
