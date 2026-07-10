import type { WorkoutSession } from "./models";

/**
 * In-browser fallback for the workout_coach capability, ported from the
 * backend's `build_session`. Never programs through pain; rest is a valid
 * choice.
 */
export function buildSession(
  goal = "general fitness",
  minutes = 30,
  equipment = "none",
): WorkoutSession {
  const total = Math.min(90, Math.max(10, Math.trunc(minutes) || 30));
  const warmup = Math.max(3, Math.trunc(total / 6));
  const cooldown = Math.max(3, Math.trunc(total / 8));
  const main = total - warmup - cooldown;

  let block: string[];
  const g = goal.toLowerCase();
  if (g.includes("strength")) {
    block = ["squats", "push-ups", "rows", "glute bridges", "plank"];
  } else if (g.includes("cardio")) {
    block = ["brisk intervals", "step-ups", "mountain climbers", "easy jog"];
  } else {
    block = ["bodyweight circuit", "core", "mobility flow"];
  }

  return {
    goal,
    totalMinutes: total,
    warmupMin: warmup,
    mainMin: main,
    cooldownMin: cooldown,
    mainBlock: block,
    equipment,
    note: "Stop if anything hurts. A rest day is a valid choice, not a broken streak.",
    timestampMillis: Date.now(),
    fromFallback: true,
    personaReaction: "",
  };
}
