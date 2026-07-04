"""health-shopping capability: goal-aligned food swaps. Never auto-purchases."""
from __future__ import annotations

from google.adk.agents import LlmAgent

from ..config import MODEL
from ..mcp_tools import nutrition_toolset
from .. import safety

# Neutral, goal-aligned swaps (never framed as "junk" or "cheat").
_SWAPS = {
    "white rice": "brown rice or basmati — similar taste, steadier energy",
    "fries": "roasted potato wedges — same comfort, less oil",
    "beef burger": "a leaner turkey or grilled-chicken burger, if you fancy it",
    "naan": "roti — lighter, still great with curry",
    "soda": "sparkling water with lime",
}


def suggest_swaps(items: list[str]) -> dict:
    """Suggest neutral, goal-aligned swaps for shopping-basket items.

    Never labels foods as good/bad; offers optional alternatives with a reason.
    """
    out = []
    for it in items:
        key = it.strip().lower()
        for k, v in _SWAPS.items():
            if k in key or key in k:
                out.append({"item": it, "swap": v})
                break
    return {"swaps": out, "note": "Suggestions only — your call, no pressure."}


SHOP_INSTRUCTION = (
    "You are the health-shopping capability. Identify foods and offer NEUTRAL, "
    "goal-aligned swaps with a short reason — never call anything 'junk', 'cheat', or "
    "'bad'. Use the nutrition lookup tool for facts. You may PREPARE a basket but you "
    "must NEVER complete a purchase or payment without explicit, per-purchase user "
    "confirmation. For clothing, restrict yourself strictly to fit, size and comfort; "
    "never comment on how an item makes the user's body look."
)

health_shopping_agent = LlmAgent(
    name="health_shopping",
    model=MODEL,
    description="Suggests goal-aligned food swaps; never purchases without confirmation.",
    instruction=SHOP_INSTRUCTION,
    tools=[suggest_swaps, nutrition_toolset()],
    before_model_callback=safety.before_model_callback,
    after_model_callback=safety.after_model_callback,
)
