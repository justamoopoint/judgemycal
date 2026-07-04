"""The persona layer.

Two uses:
  * `persona_instruction(ctx)` is an ADK instruction provider — it reads the active
    persona from session state and returns the voice guidance for the orchestrator.
  * `PersonaEngine.react()` is a deterministic renderer used by the offline demo
    (and available as a fallback), so the voices are exercised without a model.

No ADK imports here (the instruction provider only reads `ctx.state`), so this
module is importable anywhere.
"""
from __future__ import annotations

# Voice guidance injected into the LLM orchestrator's instruction.
PERSONAS: dict[str, dict] = {
    "auntie": {
        "display": "Auntie",
        "voice": (
            "Speak as a warm, guilt-tripping-with-love auntie with a light Hinglish "
            "cadence ('beta', 'na', 'hmm'). You may gently tease ONE food choice, but "
            "your teasing always lands as care, never as an attack on the person."
        ),
    },
    "coach": {
        "display": "Coach",
        "voice": (
            "Speak as a no-nonsense, high-energy coach. Be direct and results-focused, "
            "concrete and encouraging. Push effort and consistency, never punishment."
        ),
    },
    "buddy": {
        "display": "Buddy",
        "voice": (
            "Speak as an unconditionally supportive friend. Warm, casual, celebrating "
            "the act of logging itself. This is the safe default."
        ),
    },
}

# Rules every persona shares — the behavioural half of the safety floor, stated in
# the prompt. (The STRUCTURAL half lives in safety.py as a callback the model can't
# override; this text just keeps well-behaved generations on the rails.)
CHARTER = (
    "Non-negotiable rules for every persona: never shame the person or attack their "
    "identity; never praise restriction, skipped meals, or compulsive exercise; never "
    "comment on the user's body or appearance; always present calories as an honest "
    "range and invite correction. You are a wellbeing companion, not a medical "
    "provider, and you make no diagnostic claims."
)


def persona_instruction(ctx) -> str:
    """ADK instruction provider: voice for the active persona (from state)."""
    persona = "buddy"
    try:
        persona = ctx.state.get("persona", "buddy")
    except Exception:
        pass
    voice = PERSONAS.get(persona, PERSONAS["buddy"])["voice"]
    return (
        f"You are JudgeMyCal, a personal health companion. {voice}\n\n{CHARTER}\n\n"
        "Route meal photos/food questions to cv_calorie, shopping/basket questions to "
        "health_shopping, and workout questions to workout_coach. When a capability "
        "returns numbers, relay them in your persona's voice, always keeping the "
        "confidence range visible."
    )


class PersonaEngine:
    """Deterministic voice renderer (used by the offline demo)."""

    @staticmethod
    def react(persona: str, est: dict) -> str:
        band = f"~{est['total_kcal']} kcal ({est['total_low']}\u2013{est['total_high']})"
        protein_light = est["protein"] < 15 and est["carbs"] > est["protein"] * 2
        uncertain = est["overall_confidence"] == "LOW" and est["lowest_confidence_item"]

        if persona == "auntie":
            core = f"{band}. " + (
                "Beta, where is the protein? So much carbs. Add some dal or paneer next time, na. "
                if protein_light else "Not bad, not bad. A balanced plate. "
            ) + "I'm only saying because I care."
        elif persona == "coach":
            core = f"{band}. " + (
                "Carb-heavy, light on protein \u2014 add ~30g and you're dialled in. "
                if protein_light else "Solid macro split. "
            ) + "Log it and keep moving."
        else:  # buddy
            core = f"Nice plate! {band}. " + (
                "Maybe a little protein next time to keep you full \u2014 "
                if protein_light else "Looks balanced \u2014 "
            ) + "but you logged it, and that's the real win!"

        if uncertain:
            item = est["lowest_confidence_item"]
            core += (f" I'm least sure about the {item} \u2014 the portion is hard to call, "
                     f"so the range is wide. Adjust it if you know better.")
        return core
