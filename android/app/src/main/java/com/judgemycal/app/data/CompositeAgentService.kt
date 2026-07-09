package com.judgemycal.app.data

import android.util.Log
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.WorkoutSession
import kotlinx.coroutines.CancellationException

/**
 * Backend-first with graceful degradation (spec §6): a failed or timed-out
 * backend call falls back to the on-device engines instead of a dead screen.
 * `backend == null` means no backend URL is configured — pure offline demo mode.
 */
class CompositeAgentService(
    private val backend: AgentService?,
    private val fallback: AgentService,
) : AgentService {

    override suspend fun estimateMeal(
        persona: Persona,
        imageJpeg: ByteArray?,
        note: String,
    ): MealEstimate = withFallback(
        { it.estimateMeal(persona, imageJpeg, note) },
    )

    override suspend fun suggestSwaps(persona: Persona, items: List<String>): SwapResult =
        withFallback { it.suggestSwaps(persona, items) }

    override suspend fun buildWorkout(
        persona: Persona,
        goal: String,
        minutes: Int,
        equipment: String,
    ): WorkoutSession = withFallback { it.buildWorkout(persona, goal, minutes, equipment) }

    private suspend fun <T> withFallback(call: suspend (AgentService) -> T): T {
        val primary = backend ?: return call(fallback)
        return try {
            call(primary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("JudgeMyCal", "Backend call failed; using on-device fallback", e)
            call(fallback)
        }
    }
}
