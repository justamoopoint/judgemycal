"""A small, verified nutrition table (values per 100 g).

This is the ground-truth source the `cv-calorie` capability reconciles against —
in the product this lives behind an MCP server (see mcp_server/nutrition_server.py)
so calorie/macro numbers come from verified data instead of the model's memory.

Pure Python, no dependencies, so it can be imported by the MCP server, the ADK
tools, and the offline demo alike.
"""
from __future__ import annotations
from typing import Optional

# name -> (kcal, protein_g, carbs_g, fat_g) per 100 g
TABLE: dict[str, tuple[float, float, float, float]] = {
    "grilled chicken breast": (165, 31.0, 0.0, 3.6),
    "grilled chicken thigh": (209, 26.0, 0.0, 11.0),
    "chicken curry": (180, 14.0, 6.0, 11.0),
    "white rice": (130, 2.7, 28.0, 0.3),
    "basmati rice": (121, 3.0, 25.0, 0.4),
    "brown rice": (112, 2.6, 24.0, 0.9),
    "naan": (310, 9.0, 50.0, 6.0),
    "roti": (297, 11.0, 46.0, 7.0),
    "dal": (116, 9.0, 20.0, 0.4),
    "paneer": (265, 18.0, 1.2, 20.0),
    "mixed salad": (60, 1.5, 5.0, 3.5),      # includes a little dressing
    "olives": (115, 0.8, 6.0, 11.0),
    "margherita pizza": (266, 11.0, 33.0, 10.0),
    "porridge": (95, 3.0, 15.0, 2.5),        # cooked, with a little honey
    "banana": (89, 1.1, 23.0, 0.3),
    "apple": (52, 0.3, 14.0, 0.2),
    "scrambled eggs": (148, 10.0, 1.6, 11.0),
    "avocado toast": (223, 6.0, 24.0, 12.0),
    "beef burger": (244, 15.0, 20.0, 12.0),
    "fries": (312, 3.4, 41.0, 15.0),
    "salmon": (208, 20.0, 0.0, 13.0),
    "greek yogurt": (59, 10.0, 3.6, 0.4),
}

# common phrasings -> canonical key
ALIASES = {
    "chicken breast": "grilled chicken breast",
    "chicken thigh": "grilled chicken thigh",
    "rice": "white rice",
    "curry": "chicken curry",
    "pizza": "margherita pizza",
    "eggs": "scrambled eggs",
    "burger": "beef burger",
    "chips": "fries",
    "oatmeal": "porridge",
    "yogurt": "greek yogurt",
    "salad": "mixed salad",
}


def _canonical(food: str) -> Optional[str]:
    q = food.strip().lower()
    if q in TABLE:
        return q
    if q in ALIASES:
        return ALIASES[q]
    # substring match against keys, then aliases
    for key in TABLE:
        if q in key or key in q:
            return key
    for alias, key in ALIASES.items():
        if alias in q or q in alias:
            return key
    return None


def lookup(food: str, grams: float) -> dict:
    """Return scaled nutrition for `grams` of `food`.

    `matched` is False when the food isn't in the table (caller should treat the
    estimate as low confidence).
    """
    key = _canonical(food)
    if key is None:
        # Unknown food: return a neutral fallback and flag low confidence.
        kcal_per100 = 150.0
        est = kcal_per100 * grams / 100.0
        return {
            "food": food, "matched": False, "grams": grams,
            "kcal": round(est), "protein": 0, "carbs": 0, "fat": 0,
        }
    kcal, p, c, f = TABLE[key]
    factor = grams / 100.0
    return {
        "food": key, "matched": True, "grams": grams,
        "kcal": round(kcal * factor),
        "protein": round(p * factor, 1),
        "carbs": round(c * factor, 1),
        "fat": round(f * factor, 1),
    }


def search(query: str, limit: int = 8) -> list[str]:
    """Return table keys relevant to `query`."""
    q = query.strip().lower()
    hits = [k for k in TABLE if q in k]
    for alias, key in ALIASES.items():
        if q in alias and key not in hits:
            hits.append(key)
    return hits[:limit] if hits else list(TABLE.keys())[:limit]
