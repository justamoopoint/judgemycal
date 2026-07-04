"""workout-coach capability: adaptive sessions. Respects rest; refuses to train through pain."""
from __future__ import annotations

from google.adk.agents import LlmAgent

from ..config import MODEL
from .. import safety


def build_session(goal: str = "general fitness", minutes: int = 30,
                  equipment: str = "none") -> dict:
    """Build a simple workout session matched to goal, time, and equipment.

    Does NOT program through reported pain and does NOT reward overtraining;
    rest is a valid, encouraged choice.
    """
    minutes = max(10, min(int(minutes or 30), 90))
    warmup = max(3, minutes // 6)
    cooldown = max(3, minutes // 8)
    main = minutes - warmup - cooldown

    if "strength" in goal.lower():
        block = ["squats", "push-ups", "rows", "glute bridges", "plank"]
    elif "cardio" in goal.lower():
        block = ["brisk intervals", "step-ups", "mountain climbers", "easy jog"]
    else:
        block = ["bodyweight circuit", "core", "mobility flow"]

    return {
        "goal": goal, "total_minutes": minutes,
        "structure": {"warmup_min": warmup, "main_min": main, "cooldown_min": cooldown},
        "main_block": block,
        "equipment": equipment,
        "note": "Stop if anything hurts. A rest day is a valid choice, not a broken streak.",
    }


COACH_INSTRUCTION = (
    "You are the workout-coach capability. FIRST consider the user's recent history from "
    "memory for continuity, then call build_session matched to their goal, available time "
    "and equipment. Never program through pain, never reward overtraining, and treat rest "
    "as a healthy choice — offer a supportive re-entry after a gap, never a punishing "
    "make-up session."
)

workout_coach_agent = LlmAgent(
    name="workout_coach",
    model=MODEL,
    description="Builds adaptive workout sessions; respects rest and refuses to train through pain.",
    instruction=COACH_INSTRUCTION,
    tools=[build_session],
    before_model_callback=safety.before_model_callback,
    after_model_callback=safety.after_model_callback,
)
