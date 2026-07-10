import { describe, expect, it } from "vitest";
import {
  finalText,
  functionResponse,
  mealEstimate,
  swapResult,
  workoutSession,
} from "../src/data/responseMapper";
import type { WireEvent } from "../src/data/wire";

// The same fixture shape as the Android AgentResponseMapperTest — locks this
// client to the ADK 2.3 /run wire schema.
const RUN_EVENTS: WireEvent[] = JSON.parse(`
[
  {
    "id": "e1", "author": "cv_calorie",
    "content": {
      "role": "model",
      "parts": [
        {"functionCall": {"id": "fc1", "name": "estimate_meal", "args": {"image_path": "m.jpg"}}}
      ]
    }
  },
  {
    "id": "e2", "author": "cv_calorie",
    "content": {
      "role": "user",
      "parts": [
        {"functionResponse": {"id": "fc1", "name": "estimate_meal", "response": {
          "items": [
            {"name": "grilled chicken thigh", "grams": 140, "kcal": 293,
             "kcal_low": 264, "kcal_high": 322, "protein": 36.4, "carbs": 0.0,
             "fat": 15.4, "confidence": "HIGH"},
            {"name": "white rice", "grams": 180, "kcal": 234,
             "kcal_low": 152, "kcal_high": 316, "protein": 4.9, "carbs": 50.4,
             "fat": 0.5, "confidence": "LOW"}
          ],
          "total_kcal": 527, "total_low": 416, "total_high": 638,
          "protein": 41.3, "carbs": 50.4, "fat": 15.9,
          "overall_confidence": "LOW",
          "lowest_confidence_item": "white rice",
          "note": ""
        }}}
      ]
    }
  },
  {
    "id": "e3", "author": "judgemycal",
    "content": {
      "role": "model",
      "parts": [{"text": "Nice plate! ~527 kcal (416–638). You logged it, that's the win!"}]
    }
  }
]
`);

describe("responseMapper", () => {
  it("extracts the structured estimate and final text from a run event stream", () => {
    const structured = functionResponse(RUN_EVENTS, "estimate_meal");
    expect(structured).not.toBeNull();

    const text = finalText(RUN_EVENTS);
    expect(text).toContain("416");

    const est = mealEstimate(structured!, text);
    expect(est.totalKcal).toBe(527);
    expect(est.totalLow).toBe(416);
    expect(est.totalHigh).toBe(638);
    expect(est.overallConfidence).toBe("LOW");
    expect(est.lowestConfidenceItem).toBe("white rice");
    expect(est.items).toHaveLength(2);
    expect(est.items[0].confidence).toBe("HIGH");
    expect(est.fromFallback).toBe(false);
    expect(est.personaReaction).toContain("416");
  });

  it("returns null instead of inventing numbers when the tool result is missing", () => {
    expect(functionResponse(RUN_EVENTS, "build_session")).toBeNull();
  });

  it("parses swap and workout responses", () => {
    const swaps = swapResult(
      { swaps: [{ item: "fries", swap: "roasted potato wedges" }], note: "n" },
      "your call!",
    );
    expect(swaps.swaps).toHaveLength(1);
    expect(swaps.swaps[0].item).toBe("fries");

    const workout = workoutSession(
      {
        goal: "strength",
        total_minutes: 30,
        structure: { warmup_min: 5, main_min: 21, cooldown_min: 4 },
        main_block: ["squats", "push-ups"],
        equipment: "none",
        note: "Rest is valid.",
      },
      "let's go",
    );
    expect(workout.totalMinutes).toBe(30);
    expect(workout.warmupMin).toBe(5);
    expect(workout.mainBlock).toEqual(["squats", "push-ups"]);
  });
});
