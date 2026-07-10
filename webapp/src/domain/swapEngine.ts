import type { SwapResult } from "./models";

/**
 * In-browser fallback for the health_shopping capability, ported from the
 * backend's `suggest_swaps`. Neutral, goal-aligned suggestions — nothing is
 * ever labelled "junk", "cheat", or "bad".
 */

const SWAPS: Record<string, string> = {
  "white rice": "brown rice or basmati — similar taste, steadier energy",
  fries: "roasted potato wedges — same comfort, less oil",
  "beef burger": "a leaner turkey or grilled-chicken burger, if you fancy it",
  naan: "roti — lighter, still great with curry",
  soda: "sparkling water with lime",
};

export function suggestSwaps(items: string[]): SwapResult {
  const swaps = items.flatMap((item) => {
    const key = item.trim().toLowerCase();
    const hit = Object.entries(SWAPS).find(
      ([k]) => key.includes(k) || k.includes(key),
    );
    return hit ? [{ item, swap: hit[1] }] : [];
  });
  return {
    swaps,
    note: "Suggestions only — your call, no pressure.",
    fromFallback: true,
    personaReaction: "",
  };
}
