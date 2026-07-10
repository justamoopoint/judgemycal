import type { Confidence, MealEstimate, MealItem } from "./models";
import { lookup } from "./nutritionDb";

/**
 * In-browser fallback estimator, a faithful port of the backend's
 * `estimator.py`: mock recognition + real reconciliation against the nutrition
 * table. Bands widen with uncertainty, and the meal's overall confidence is
 * the LOWEST item's — never an average. Results are flagged `fromFallback`.
 */

const BAND: Record<Confidence, number> = { HIGH: 0.1, MEDIUM: 0.2, LOW: 0.35 };
const ORDER: Record<Confidence, number> = { HIGH: 0, MEDIUM: 1, LOW: 2 };

type Recognized = [food: string, grams: number, confidence: Confidence];

const SAMPLES: Recognized[][] = [
  [
    ["grilled chicken thigh", 140, "HIGH"],
    ["white rice", 180, "LOW"],
    ["mixed salad", 90, "MEDIUM"],
  ],
  [
    ["margherita pizza", 220, "MEDIUM"],
    ["olives", 40, "HIGH"],
  ],
  [
    ["porridge", 300, "HIGH"],
    ["banana", 120, "HIGH"],
  ],
  [
    ["chicken curry", 250, "LOW"],
    ["naan", 90, "MEDIUM"],
    ["basmati rice", 150, "LOW"],
  ],
  [
    ["scrambled eggs", 150, "HIGH"],
    ["avocado toast", 130, "MEDIUM"],
  ],
  [
    ["beef burger", 250, "MEDIUM"],
    ["fries", 130, "LOW"],
  ],
];

const round1 = (v: number) => Math.round(v * 10) / 10;

function itemFor(food: string, grams: number, confidence: Confidence): MealItem {
  const row = lookup(food, grams);
  // An unmatched food can never be better than LOW confidence.
  const conf: Confidence = row.matched ? confidence : "LOW";
  const band = BAND[conf];
  return {
    name: row.food,
    grams,
    kcal: row.kcal,
    kcalLow: Math.round(row.kcal * (1 - band)),
    kcalHigh: Math.round(row.kcal * (1 + band)),
    protein: row.protein,
    carbs: row.carbs,
    fat: row.fat,
    confidence: conf,
  };
}

function fromItems(items: MealItem[], note: string): MealEstimate {
  const overall = items.reduce<Confidence>(
    (worst, i) => (ORDER[i.confidence] > ORDER[worst] ? i.confidence : worst),
    "HIGH",
  );
  const lows = items.filter((i) => i.confidence === "LOW");
  const lowest =
    lows.length > 0
      ? lows.reduce((min, i) => (i.kcal < min.kcal ? i : min)).name
      : null;
  return {
    items,
    totalKcal: items.reduce((s, i) => s + i.kcal, 0),
    totalLow: items.reduce((s, i) => s + i.kcalLow, 0),
    totalHigh: items.reduce((s, i) => s + i.kcalHigh, 0),
    protein: round1(items.reduce((s, i) => s + i.protein, 0)),
    carbs: round1(items.reduce((s, i) => s + i.carbs, 0)),
    fat: round1(items.reduce((s, i) => s + i.fat, 0)),
    overallConfidence: overall,
    lowestConfidenceItem: lowest,
    note,
    fromFallback: true,
    personaReaction: "",
  };
}

export function estimate(seed: number = Date.now(), note = ""): MealEstimate {
  const sample = SAMPLES[((seed % SAMPLES.length) + SAMPLES.length) % SAMPLES.length];
  return fromItems(
    sample.map(([food, grams, conf]) => itemFor(food, grams, conf)),
    note,
  );
}

/** Rebuild the meal after a one-tap portion correction on a single item. */
export function withPortion(
  est: MealEstimate,
  itemName: string,
  grams: number,
): MealEstimate {
  const items = est.items.map((item) => {
    if (item.name !== itemName) return item;
    // The user just told us the portion — that item is now HIGH confidence.
    return itemFor(item.name, grams, "HIGH");
  });
  return {
    ...fromItems(items, est.note),
    fromFallback: est.fromFallback,
    personaReaction: est.personaReaction,
  };
}
