---
name: health-shopping
description: Analyse what the user is buying and suggest goal-aligned choices. Use this skill whenever the user scans, photographs, or shares a grocery basket, receipt, shopping list, or a packaged product / barcode and wants it assessed — including "should I buy this", "is this healthy", "find me a better swap", "scan my basket", or comparing two products. Also use for the clothing sub-mode when the user asks about clothing size, fit, or comfort for their training goals. Prefer this skill over cv-calorie for packaged products and barcodes. Do not use it to place an order or take payment without explicit user confirmation.
version: 1.0.0
---

# Health Shopping

Help the user shop in line with their goals — flagging nutritional anomalies in food purchases and proposing better swaps, or giving fit/comfort guidance on clothing. Like every capability in this product, this skill returns **persona-neutral structured data**; the persona layer gives it a voice and the safety guard governs that voice. This skill is the highest-risk surface in the product, because commentary on what someone buys and wears tips into shaming faster than meal commentary does. The boundaries below are not optional.

## Mode selection

Pick one mode from the input:
- **food** — a basket, receipt, product photo, barcode, or shopping list. Default when food or groceries are involved.
- **clothes** — a garment, a size question, or a fit/comfort question tied to the user's body or training.

If genuinely ambiguous, default to **food**.

---

## FOOD mode

### Inputs
- A basket/receipt photo, a product image, a scanned barcode, or a text list.
- Optional context (use if present): the user's goal (e.g. higher protein, lower added sugar), allergies, and dietary pattern.

### Steps

1. **Identify products.** List each distinct product. For a barcode, resolve it to a specific product; for a basket or receipt, enumerate items.
2. **Look up nutrition.** Query the **`nutrition-db` MCP server** (and the barcode resolver for packaged goods) for per-serving and per-100g calories, macros, and key flags (added sugar, saturated fat, sodium, fibre, protein).
3. **Flag against goals.** Identify items that run counter to the user's stated goal — e.g. a cereal that is mostly added sugar for a user targeting lower sugar. Flag on the *nutrition*, neutrally. Do not label foods "bad", "junk", "cheat", or "guilty"; describe what they are ("high in added sugar per serving"), not a verdict on the person for buying them.
4. **Propose swaps.** For each flagged item, suggest one or two concrete, closer-to-goal alternatives in the same category (a lower-sugar cereal, a higher-protein yoghurt), with the nutritional reason. Swaps are suggestions, never instructions.

### Purchase boundary (critical)

This skill may **prepare** a cart or shortlist (via the merchant catalogue over UCP), but it must **never execute a payment or place an order on its own**. Purchasing with a payment method on file requires explicit, per-purchase user confirmation. Surface the proposed cart and its total, and stop — the user (not the agent) authorises the actual purchase. Never store, request, or enter payment credentials in this flow.

### FOOD output contract

Return only this JSON — no prose:

```json
{
  "mode": "food",
  "products": [
    {
      "name": "Brand X Frosted Cereal",
      "serving_g": 40,
      "calories_per_serving": 160,
      "flags": ["high_added_sugar"],
      "macros_g": { "protein": 2, "carbs": 34, "fat": 1, "added_sugar": 18 }
    }
  ],
  "suggested_swaps": [
    {
      "for": "Brand X Frosted Cereal",
      "alternative": "plain rolled oats",
      "reason": "≈1g added sugar per serving vs 18g; higher fibre",
      "same_category": true
    }
  ],
  "prepared_cart": null,
  "notes_for_persona": ["basket is well-aligned overall; only the cereal runs counter to the lower-sugar goal"],
  "safety_flags": []
}
```

---

## CLOTHES mode

This mode exists to help with **fit, comfort, and utility only**. It runs under the strictest reading of the safety floor.

### Absolute constraints

- Talk about **fit, size, fabric, comfort, range of motion, and suitability for an activity** — nothing else.
- **Never** comment on appearance, attractiveness, how something "flatters", "slims", "hides", or "shows off" any part of the body.
- **Never** frame clothing as something to be "earned", or tie it to weight, body change, or a goal weight ("this will fit once you…").
- **Never** compare the user's body to any standard, and never infer or comment on body shape or size beyond the neutral measurements needed to recommend a size.
- If the user's own message frames the request around appearance or body dissatisfaction, do not engage on those terms — set a `safety_flag` and let the guard respond. Provide only neutral fit information.

### Steps
1. Determine the garment and the activity or context it is for.
2. Use the user's stated measurements or size (ask for a measurement only if needed for sizing; never estimate body size from a photo of the person).
3. Give sizing guidance (size, fit type — relaxed/regular/compression), fabric/comfort notes, and suitability for the intended activity (e.g. breathable and stretchy for mobility work).

### CLOTHES output contract

```json
{
  "mode": "clothes",
  "garment": "training shorts",
  "activity": "strength + mobility",
  "fit_guidance": {
    "recommended_size": "M",
    "fit_type": "regular, slight stretch",
    "fabric_notes": "breathable, four-way stretch supports full squat depth",
    "sizing_reason": "based on stated 32in waist and a regular-fit preference"
  },
  "notes_for_persona": ["comfort and mobility framing only"],
  "safety_flags": []
}
```

---

## Hard boundaries (both modes)

- Never moralise about purchases or appearance. Flag nutrition neutrally; discuss clothing as fit only.
- Never execute a payment or place an order without explicit user confirmation; never handle payment credentials.
- Never estimate a person's body size or shape from an image for the purpose of commentary.
- Pass any wellbeing concern in the *user's message* to `safety_flags` for the guard; do not attempt to reassure or counsel from within this skill.

## Tools and dependencies

- **`nutrition-db` MCP server** and a barcode resolver — product nutrition lookup.
- Merchant catalogue over **UCP** — for building a shortlist/cart (read and prepare only).
- **AP2** payment mandates — cart authorisation is handled by the orchestrator **only after explicit user confirmation**; this skill never triggers it autonomously.

## Deeper reference

Load `references/swap-catalogue.md` for category-by-category swap suggestions, and `references/clothes-safety.md` for the full list of prohibited framings and neutral-language patterns, when a request goes beyond the common cases above.
