"""The pure core of the `cv-calorie` capability.

The vision step is mocked (it picks a plausible meal) but the nutrition step is
real (every item is reconciled against nutrition_db). Each item carries a
confidence, and — crucially — the meal's overall confidence is the LOWEST item's,
never an average, because one shaky estimate makes the whole total uncertain.

No third-party dependencies, so the offline demo and the ADK tool share this code.
"""
from __future__ import annotations
from typing import Optional
from . import nutrition_db

# Band width by confidence: how far the low/high range spreads around the point.
_BAND = {"HIGH": 0.10, "MEDIUM": 0.20, "LOW": 0.35}

# Mock "recognition" results: each meal is a list of (food, grams, portion_confidence).
# Portion confidence reflects how hard the portion is to judge from a photo
# (rice/curry are notoriously hard; a countable banana is easy).
_SAMPLES: list[list[tuple[str, int, str]]] = [
    [("grilled chicken thigh", 140, "HIGH"), ("white rice", 180, "LOW"),
     ("mixed salad", 90, "MEDIUM")],
    [("margherita pizza", 220, "MEDIUM"), ("olives", 40, "HIGH")],
    [("porridge", 300, "HIGH"), ("banana", 120, "HIGH")],
    [("chicken curry", 250, "LOW"), ("naan", 90, "MEDIUM"), ("basmati rice", 150, "LOW")],
    [("scrambled eggs", 150, "HIGH"), ("avocado toast", 130, "MEDIUM")],
    [("beef burger", 250, "MEDIUM"), ("fries", 130, "LOW")],
]

_ORDER = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}


def _pick_index(image_path: Optional[str]) -> int:
    import time
    seed = (hash(image_path) if image_path else 0) + int(time.time())
    return seed % len(_SAMPLES)


def estimate(image_path: Optional[str] = None, note: Optional[str] = None) -> dict:
    """Produce a persona-neutral meal estimate with per-item + overall confidence."""
    sample = _SAMPLES[_pick_index(image_path)]
    items = []
    for food, grams, conf in sample:
        row = nutrition_db.lookup(food, grams)
        # An unmatched food can never be better than LOW confidence.
        if not row["matched"]:
            conf = "LOW"
        band = _BAND[conf]
        items.append({
            "name": row["food"], "grams": grams,
            "kcal": row["kcal"],
            "kcal_low": round(row["kcal"] * (1 - band)),
            "kcal_high": round(row["kcal"] * (1 + band)),
            "protein": row["protein"], "carbs": row["carbs"], "fat": row["fat"],
            "confidence": conf,
        })

    total = sum(i["kcal"] for i in items)
    low = sum(i["kcal_low"] for i in items)
    high = sum(i["kcal_high"] for i in items)
    overall = max(items, key=lambda i: _ORDER[i["confidence"]])["confidence"]
    lows = [i for i in items if i["confidence"] == "LOW"]
    lowest_item = (min(lows, key=lambda i: i["kcal"])["name"] if lows else None)

    return {
        "items": items,
        "total_kcal": total, "total_low": low, "total_high": high,
        "protein": round(sum(i["protein"] for i in items), 1),
        "carbs": round(sum(i["carbs"] for i in items), 1),
        "fat": round(sum(i["fat"] for i in items), 1),
        "overall_confidence": overall,
        "lowest_confidence_item": lowest_item,
        "note": note or "",
    }
