import type {
  Confidence,
  MealEstimate,
  MealItem,
  SwapResult,
  WorkoutSession,
} from "../domain/models";
import type { WireEvent } from "./wire";

/**
 * Extracts structured capability results from an ADK /run event stream. The
 * agent's tools return persona-neutral dicts; those arrive as functionResponse
 * parts alongside the persona-voiced final text. Reading structure from the
 * tool response — not by parsing prose — keeps the confidence-band UI honest.
 */

export function finalText(events: WireEvent[]): string {
  for (let i = events.length - 1; i >= 0; i--) {
    const event = events[i];
    if (event.partial === true) continue;
    const text = (event.content?.parts ?? [])
      .map((p) => p.text ?? "")
      .join("");
    if (text.trim().length > 0) return text;
  }
  return "";
}

export function functionResponse(
  events: WireEvent[],
  toolName: string,
): Record<string, unknown> | null {
  for (let i = events.length - 1; i >= 0; i--) {
    for (const part of events[i].content?.parts ?? []) {
      const fr = part.functionResponse;
      if (fr?.name === toolName && fr.response) return fr.response;
    }
  }
  return null;
}

const str = (o: Record<string, unknown>, k: string): string =>
  typeof o[k] === "string" ? (o[k] as string) : "";
const num = (o: Record<string, unknown>, k: string): number =>
  typeof o[k] === "number" ? (o[k] as number) : 0;

function confidence(value: string): Confidence {
  const v = value.toUpperCase();
  return v === "HIGH" || v === "MEDIUM" ? (v as Confidence) : "LOW";
}

export function mealEstimate(
  response: Record<string, unknown>,
  personaReaction: string,
): MealEstimate {
  const rawItems = Array.isArray(response.items) ? response.items : [];
  const items: MealItem[] = rawItems.map((el) => {
    const o = el as Record<string, unknown>;
    return {
      name: str(o, "name"),
      grams: num(o, "grams"),
      kcal: num(o, "kcal"),
      kcalLow: num(o, "kcal_low"),
      kcalHigh: num(o, "kcal_high"),
      protein: num(o, "protein"),
      carbs: num(o, "carbs"),
      fat: num(o, "fat"),
      confidence: confidence(str(o, "confidence")),
    };
  });
  return {
    items,
    totalKcal: num(response, "total_kcal"),
    totalLow: num(response, "total_low"),
    totalHigh: num(response, "total_high"),
    protein: num(response, "protein"),
    carbs: num(response, "carbs"),
    fat: num(response, "fat"),
    overallConfidence: confidence(str(response, "overall_confidence")),
    lowestConfidenceItem:
      typeof response.lowest_confidence_item === "string"
        ? response.lowest_confidence_item
        : null,
    note: str(response, "note"),
    fromFallback: false,
    personaReaction,
  };
}

export function swapResult(
  response: Record<string, unknown>,
  personaReaction: string,
): SwapResult {
  const rawSwaps = Array.isArray(response.swaps) ? response.swaps : [];
  return {
    swaps: rawSwaps.map((el) => {
      const o = el as Record<string, unknown>;
      return { item: str(o, "item"), swap: str(o, "swap") };
    }),
    note: str(response, "note"),
    fromFallback: false,
    personaReaction,
  };
}

export function workoutSession(
  response: Record<string, unknown>,
  personaReaction: string,
): WorkoutSession {
  const structure = (response.structure ?? {}) as Record<string, unknown>;
  const rawBlock = Array.isArray(response.main_block) ? response.main_block : [];
  return {
    goal: str(response, "goal"),
    totalMinutes: num(response, "total_minutes"),
    warmupMin: num(structure, "warmup_min"),
    mainMin: num(structure, "main_min"),
    cooldownMin: num(structure, "cooldown_min"),
    mainBlock: rawBlock.filter((x): x is string => typeof x === "string"),
    equipment: str(response, "equipment"),
    note: str(response, "note"),
    timestampMillis: Date.now(),
    fromFallback: false,
    personaReaction,
  };
}
