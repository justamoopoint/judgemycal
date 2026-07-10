import type { MealEstimate, Persona } from "./models";

export const PERSONA_LABELS: Record<Persona, string> = {
  buddy: "Buddy",
  auntie: "Auntie",
  coach: "Coach",
};

export const PERSONA_DESCRIPTIONS: Record<Persona, string> = {
  buddy: "Unconditionally supportive. The default.",
  auntie: "Guilt-trips you with love. Opt-in.",
  coach: "No-nonsense, high energy. Opt-in.",
};

/**
 * Deterministic persona voice renderer, ported from the backend's
 * `PersonaEngine.react()`. Used by the offline fallback; online, the voiced
 * reply comes from the agent.
 */
export function react(persona: Persona, est: MealEstimate): string {
  const band = `~${est.totalKcal} kcal (${est.totalLow}–${est.totalHigh})`;
  const proteinLight = est.protein < 15 && est.carbs > est.protein * 2;
  const uncertain =
    est.overallConfidence === "LOW" && est.lowestConfidenceItem !== null;

  let core: string;
  if (persona === "auntie") {
    core =
      `${band}. ` +
      (proteinLight
        ? "Beta, where is the protein? So much carbs. Add some dal or paneer next time, na. "
        : "Not bad, not bad. A balanced plate. ") +
      "I'm only saying because I care.";
  } else if (persona === "coach") {
    core =
      `${band}. ` +
      (proteinLight
        ? "Carb-heavy, light on protein — add ~30g and you're dialled in. "
        : "Solid macro split. ") +
      "Log it and keep moving.";
  } else {
    core =
      `Nice plate! ${band}. ` +
      (proteinLight
        ? "Maybe a little protein next time to keep you full — "
        : "Looks balanced — ") +
      "but you logged it, and that's the real win!";
  }

  if (uncertain) {
    core +=
      ` I'm least sure about the ${est.lowestConfidenceItem} — the portion is ` +
      "hard to call, so the range is wide. Adjust it if you know better.";
  }
  return core;
}
