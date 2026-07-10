package com.judgemycal.app.domain

/**
 * Deterministic persona voice renderer, ported from the backend's
 * `PersonaEngine.react()`. Used by the offline fallback so persona reactions
 * work without a network; online, the voiced reply comes from the agent.
 */
object PersonaEngine {

    fun react(persona: Persona, est: MealEstimate): String {
        val band = "~${est.totalKcal} kcal (${est.totalLow}–${est.totalHigh})"
        val proteinLight = est.protein < 15 && est.carbs > est.protein * 2
        val uncertain = est.overallConfidence == Confidence.LOW && est.lowestConfidenceItem != null

        var core = when (persona) {
            Persona.AUNTIE -> "$band. " + (
                if (proteinLight) {
                    "Beta, where is the protein? So much carbs. Add some dal or paneer next time, na. "
                } else {
                    "Not bad, not bad. A balanced plate. "
                }
                ) + "I'm only saying because I care."

            Persona.COACH -> "$band. " + (
                if (proteinLight) {
                    "Carb-heavy, light on protein — add ~30g and you're dialled in. "
                } else {
                    "Solid macro split. "
                }
                ) + "Log it and keep moving."

            Persona.BUDDY -> "Nice plate! $band. " + (
                if (proteinLight) {
                    "Maybe a little protein next time to keep you full — "
                } else {
                    "Looks balanced — "
                }
                ) + "but you logged it, and that's the real win!"
        }

        if (uncertain) {
            core += " I'm least sure about the ${est.lowestConfidenceItem} — the portion is " +
                "hard to call, so the range is wide. Adjust it if you know better."
        }
        return core
    }
}
