export type Confidence = "HIGH" | "MEDIUM" | "LOW";

export type Persona = "buddy" | "auntie" | "coach";

/** Buddy is the safe default; Auntie/Coach are opt-in. */
export const DEFAULT_PERSONA: Persona = "buddy";

export function personaFromKey(key: string | null | undefined): Persona {
  return key === "auntie" || key === "coach" ? key : DEFAULT_PERSONA;
}

export interface MealItem {
  name: string;
  grams: number;
  kcal: number;
  kcalLow: number;
  kcalHigh: number;
  protein: number;
  carbs: number;
  fat: number;
  confidence: Confidence;
}

/** A persona-neutral estimate. Always a range — never a fake-precise number. */
export interface MealEstimate {
  items: MealItem[];
  totalKcal: number;
  totalLow: number;
  totalHigh: number;
  protein: number;
  carbs: number;
  fat: number;
  overallConfidence: Confidence;
  lowestConfidenceItem: string | null;
  note: string;
  /** True when produced by the in-browser fallback engine instead of the backend. */
  fromFallback: boolean;
  personaReaction: string;
}

export interface LoggedMeal {
  name: string;
  kcal: number;
  kcalLow: number;
  kcalHigh: number;
  timestampMillis: number;
}

export interface SwapSuggestion {
  item: string;
  swap: string;
}

export interface SwapResult {
  swaps: SwapSuggestion[];
  note: string;
  fromFallback: boolean;
  personaReaction: string;
}

export interface WorkoutSession {
  goal: string;
  totalMinutes: number;
  warmupMin: number;
  mainMin: number;
  cooldownMin: number;
  mainBlock: string[];
  equipment: string;
  note: string;
  timestampMillis: number;
  fromFallback: boolean;
  personaReaction: string;
}
