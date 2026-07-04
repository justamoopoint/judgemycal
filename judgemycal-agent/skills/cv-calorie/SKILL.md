---
name: cv-calorie
description: Estimate calories and macronutrients from a photo of food. Use this skill whenever the user shares, uploads, or refers to an image of a meal, snack, drink, or plate and wants it logged or analysed — including phrases like "what's in this", "how many calories", "log this meal", "add this to my day", or any time a food photo appears with intent to track it. Also use when the user describes a meal in text and asks for an estimate. Do not use for packaged-product barcodes (that is the health-shopping skill) or for pure nutrition-fact questions with no meal to analyse.
version: 1.0.0
---

# CV Calorie Estimation

Turn a photo (or text description) of a meal into an honest, structured calorie and macronutrient estimate. This skill produces **persona-neutral structured data only** — it never speaks to the user directly, never praises or judges a food choice, and never gives dieting advice. The orchestrator hands your output to the persona layer (which gives it a voice) and the safety guard (which governs that voice). Your one job is an accurate, well-calibrated estimate with its uncertainty made explicit.

## Why this skill is built the way it is

Photo calorie estimation is a four-stage pipeline — segmentation → identification → portion estimation → nutrient lookup — and small errors compound across stages. Portion estimation is the weakest link by a wide margin: identification can be 80%+ accurate while portion error alone can exceed 40%. Two things follow, and they shape every step below:

1. **Never emit a single false-precise number.** Always return a range and a confidence level. A meal is "~620 kcal (520–740)", never "620 kcal". Downstream, this honesty is what lets the companion be trusted; a number that looks wrong with no admitted uncertainty is what destroys trust.
2. **Instrument portion the hardest.** Use every available scale cue, make portion the most inspectable part of the output, and lower confidence aggressively when scale is ambiguous.

## Inputs

- **Required:** one food image (from camera or upload), OR a text description of a meal when no image is available.
- **Optional context (use if provided, never block on it):** the user's stated goal, known allergies/diet, a size reference the user mentions ("the plate is 27cm"), or a correction from a previous turn on the same meal.

## Execution steps

Follow these in order. Do not skip the food-gate or the reconciliation step.

### 1. Verify food is present

Confirm the image actually contains food. If it does not (a person, a landscape, a blank frame), do **not** hallucinate a meal. Return an empty `items` array with `status: "no_food_detected"` and stop. Never invent food that isn't visible.

### 2. Detect and separate each item

Identify every distinct food component on the plate as its own item. A burger, its fries, and a drink are three items, not one "burger meal". Composite dishes (a curry, a salad, a stir-fry) are a single item unless clearly separable components are visible. Be specific in naming ("grilled chicken thigh", not "meat") because specificity drives a better nutrient match in step 5.

### 3. Estimate portion using scale references

This is the highest-error step — treat it with the most care. Estimate the mass of each item in grams, anchoring to any visible scale reference in this priority order:

1. A reference object the user named, or a standard dinner plate (~26–28cm) if a full plate edge is visible.
2. Standard cutlery in frame (a dinner fork ≈ 18–20cm, a teaspoon bowl ≈ 5ml) as a ruler.
3. A hand, if visible (a closed fist ≈ 1 cup, a thumb tip ≈ 1 tablespoon, a palm ≈ a 3oz protein portion).
4. If **no** scale reference is available, fall back to typical single-serving sizes for the dish — and drop that item's confidence to `low`, because unreferenced portion estimates are the single largest source of error.

Account for what you cannot see: food hidden behind other food, the depth of a bowl, sauces and cooking oils that add calories without visible bulk. When in doubt, widen the range rather than guessing precisely.

### 4. Two-pass refinement

Do not return the first pass. After your initial identification and portion estimate, re-examine the whole plate once and sanity-check against nutritional logic:
- Do the portions sum to a plausible meal for this context (a snack vs. a full dinner)?
- Did you miss a likely-present-but-hard-to-see ingredient (dressing on a salad, butter on toast, oil in a stir-fry)?
- Is any single item's estimate implausible on its own?

Adjust, then proceed. This second pass measurably improves consistency.

### 5. Reconcile against the verified nutrition database (MCP)

Do not trust raw calorie/macro numbers from the vision model. For each identified item, query the nutrition database over the **`nutrition-db` MCP server** to get canonical per-100g calorie and macronutrient values, then scale by your estimated grams. This grounds the final numbers in real reference data rather than model recall.

- Match on the most specific name you can. If the DB returns several candidates, choose the closest and note reduced confidence if the match is loose.
- If the item has no DB match at all, estimate from the nearest analogue and set that item's `db_match: "approximate"` with lowered confidence.

### 6. Compute confidence

Assign every item and the meal total a confidence of `high`, `medium`, or `low`, and a numeric calorie range. Use this rubric:

- **high** — item clearly identified, a good scale reference present, clean DB match. Range within roughly ±15% of the point estimate.
- **medium** — some ambiguity in identification OR portion OR DB match. Range roughly ±25%.
- **low** — no scale reference, occluded/ambiguous item, or approximate DB match. Range ±40% or wider.

The meal's overall confidence is the lowest item confidence, not the average — one badly-estimated item makes the whole total uncertain, and pretending otherwise is dishonest.

## Output contract

Return **only** the JSON object below — no prose, no markdown fences, no commentary. The persona layer produces all user-facing language; if you editorialise here, you break the separation the product depends on.

```json
{
  "status": "ok",
  "items": [
    {
      "name": "grilled chicken thigh",
      "grams": 140,
      "calories": 250,
      "calories_low": 215,
      "calories_high": 290,
      "macros_g": { "protein": 28, "carbs": 0, "fat": 15 },
      "confidence": "high",
      "portion_basis": "palm reference in frame",
      "db_match": "exact"
    }
  ],
  "meal_total": {
    "calories": 620,
    "calories_low": 520,
    "calories_high": 740,
    "macros_g": { "protein": 42, "carbs": 55, "fat": 24 },
    "confidence": "medium"
  },
  "lowest_confidence_item": "white rice",
  "notes_for_persona": ["portion of rice was unreferenced — flagged as the item most worth confirming"],
  "safety_flags": []
}
```

Field notes:
- `notes_for_persona` — neutral, factual hints the persona layer can voice (e.g. which item to ask the user to confirm). Never opinions about the food's healthiness.
- `safety_flags` — leave empty unless the *user's accompanying message* (not the food itself) contains a wellbeing concern; if so, pass a neutral flag (e.g. `"restriction_language_in_caption"`) so the safety guard can act. Do **not** infer distress from the food alone — logging a small meal or a large meal is not itself a concern.
- On no food: `{"status": "no_food_detected", "items": [], ...}`.

## Hard boundaries

- Never praise, shame, or moralise about a food choice — that is not this skill's job and violates the product's safety design.
- Never give calorie targets, deficits, or dieting instructions.
- Never claim more certainty than the evidence supports. If you cannot estimate an item, say so via `low` confidence and a wide range rather than guessing precisely.
- Never fabricate food that is not visible in the image.

## Tools and dependencies

- **`nutrition-db` MCP server** — canonical per-100g calorie and macro reference (required for step 5).
- Multimodal vision model for identification and portion estimation (provided by the orchestrator).

## Deeper reference

For extended portion heuristics by food class (liquids, grains, spreads, composite dishes) and the full confidence-calibration table, load `references/portion-heuristics.md` when a plate is unusually complex or contains items the standard heuristics above do not cover.
