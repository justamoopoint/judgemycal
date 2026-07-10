package com.judgemycal.app.domain

/**
 * On-device fallback for the workout_coach capability, ported from the
 * backend's `build_session`. Never programs through pain, never rewards
 * overtraining; rest is a valid choice.
 */
object WorkoutBuilder {

    fun buildSession(
        goal: String = "general fitness",
        minutes: Int = 30,
        equipment: String = "none",
    ): WorkoutSession {
        val total = minutes.coerceIn(10, 90)
        val warmup = maxOf(3, total / 6)
        val cooldown = maxOf(3, total / 8)
        val main = total - warmup - cooldown

        val block = when {
            goal.contains("strength", ignoreCase = true) ->
                listOf("squats", "push-ups", "rows", "glute bridges", "plank")
            goal.contains("cardio", ignoreCase = true) ->
                listOf("brisk intervals", "step-ups", "mountain climbers", "easy jog")
            else -> listOf("bodyweight circuit", "core", "mobility flow")
        }

        return WorkoutSession(
            goal = goal,
            totalMinutes = total,
            warmupMin = warmup,
            mainMin = main,
            cooldownMin = cooldown,
            mainBlock = block,
            equipment = equipment,
            note = "Stop if anything hurts. A rest day is a valid choice, not a broken streak.",
            timestampMillis = System.currentTimeMillis(),
            fromFallback = true,
        )
    }
}
