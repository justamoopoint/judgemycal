"""JudgeMyCal root orchestrator (ADK entry point).

Multi-agent design:
    judgemycal (orchestrator)
        ├─ cv_calorie       (photo -> honest calorie estimate; MCP nutrition tools)
        ├─ health_shopping  (goal-aligned swaps; never auto-purchases)
        └─ workout_coach    (adaptive sessions; respects rest)

The orchestrator's *voice* is set at runtime by the active persona (from session
state). The *safety floor* is attached as before/after-model callbacks on EVERY
agent in the tree — so it holds no matter which agent is answering, and no persona
can override it. This is the "persona proposes, guard disposes" design in code.

`adk run judgemycal` and `adk web` discover `root_agent` below.
"""
from __future__ import annotations

from google.adk.agents import LlmAgent

from .config import MODEL
from .personas import persona_instruction, CHARTER
from . import safety
from .capabilities import cv_calorie_agent, health_shopping_agent, workout_coach_agent

root_agent = LlmAgent(
    name="judgemycal",
    model=MODEL,
    description="Personality-led health companion that routes to calorie, shopping, and workout capabilities.",
    # Voice is chosen per-request from the active persona in session state.
    instruction=persona_instruction,
    # A charter that applies to the whole app, above any persona.
    global_instruction=CHARTER,
    sub_agents=[cv_calorie_agent, health_shopping_agent, workout_coach_agent],
    # The non-overridable safety floor.
    before_model_callback=safety.before_model_callback,
    after_model_callback=safety.after_model_callback,
)
