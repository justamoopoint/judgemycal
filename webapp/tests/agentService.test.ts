import { describe, expect, it } from "vitest";
import {
  CompositeAgentService,
  FallbackAgentService,
  type AgentService,
} from "../src/data/agentService";

const failingBackend: AgentService = {
  estimateMeal: () => Promise.reject(new Error("network down")),
  suggestSwaps: () => Promise.reject(new Error("network down")),
  buildWorkout: () => Promise.reject(new Error("network down")),
};

describe("CompositeAgentService", () => {
  it("degrades to a flagged in-browser fallback, not a dead screen", async () => {
    const composite = new CompositeAgentService(
      failingBackend,
      new FallbackAgentService(),
    );

    const est = await composite.estimateMeal("buddy", null, "");
    expect(est.fromFallback).toBe(true);
    expect(est.totalLow).toBeLessThan(est.totalKcal);
    expect(est.totalKcal).toBeLessThan(est.totalHigh);
    expect(est.personaReaction.length).toBeGreaterThan(0);

    const workout = await composite.buildWorkout("coach", "strength", 30, "none");
    expect(workout.fromFallback).toBe(true);
    expect(workout.totalMinutes).toBe(30);

    const swaps = await composite.suggestSwaps("buddy", ["fries"]);
    expect(swaps.fromFallback).toBe(true);
    expect(swaps.swaps).toHaveLength(1);
  });

  it("no backend configured means pure offline mode", async () => {
    const composite = new CompositeAgentService(null, new FallbackAgentService());
    const est = await composite.estimateMeal("buddy", null, "");
    expect(est.fromFallback).toBe(true);
  });
});
