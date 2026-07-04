"""cv-calorie capability: photo -> honest calorie/macro estimate, logged to memory.

The estimate tool uses the pure estimator (mock vision + real nutrition reconciliation)
and writes the result to the persona-agnostic memory bus. The agent also carries the
nutrition MCP toolset, so it can verify individual items against ground-truth data.
"""
from __future__ import annotations
from typing import Optional

from google.adk.agents import LlmAgent

from ..config import MODEL
from ..estimator import estimate as _estimate
from ..memory import add_meal
from ..mcp_tools import nutrition_toolset
from .. import safety


def estimate_meal(image_path: str = "", note: str = "", tool_context=None) -> dict:
    """Estimate calories and macros for a meal photo and log it.

    Args:
        image_path: Path/URI of the meal photo (optional in demo mode).
        note: Optional free-text hint from the user (e.g. "no oil").
    Returns:
        A persona-neutral estimate: items with per-item confidence, a total calorie
        RANGE, macros, the overall confidence (the lowest item's), and the
        lowest-confidence item. Always relay the range, never a single fake-precise number.
    """
    est = _estimate(image_path or None, note or None)
    if tool_context is not None:
        try:
            first = est["items"][0]["name"] if est["items"] else "meal"
            add_meal(tool_context.state, {"name": first, "kcal": est["total_kcal"]})
        except Exception:
            pass
    return est


CV_INSTRUCTION = (
    "You are the cv-calorie capability. Call estimate_meal to analyse a meal photo. "
    "Portion size is the biggest error source, so ALWAYS surface the calorie range and "
    "the item you're least sure about; never present a single fake-precise number. If a "
    "specific item's identity or portion is in doubt, verify it with the nutrition "
    "lookup tool before reporting. Return the numbers plainly; the orchestrator adds voice."
)

cv_calorie_agent = LlmAgent(
    name="cv_calorie",
    model=MODEL,
    description="Estimates calories and macros from a meal photo, with honest confidence bands.",
    instruction=CV_INSTRUCTION,
    tools=[estimate_meal, nutrition_toolset()],
    before_model_callback=safety.before_model_callback,
    after_model_callback=safety.after_model_callback,
)
