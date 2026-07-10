import { describe, expect, it } from "vitest";
import { estimate, withPortion } from "../src/domain/calorieEstimator";

const estimates = Array.from({ length: 25 }, (_, i) => estimate(i));

describe("calorieEstimator", () => {
  it("every estimate is a range, never a point", () => {
    for (const est of estimates) {
      expect(est.totalLow).toBeLessThan(est.totalKcal);
      expect(est.totalKcal).toBeLessThan(est.totalHigh);
    }
  });

  it("totals are item sums", () => {
    for (const est of estimates) {
      expect(est.totalKcal).toBe(est.items.reduce((s, i) => s + i.kcal, 0));
      expect(est.totalLow).toBe(est.items.reduce((s, i) => s + i.kcalLow, 0));
      expect(est.totalHigh).toBe(est.items.reduce((s, i) => s + i.kcalHigh, 0));
    }
  });

  it("overall confidence is the lowest item's, never an average", () => {
    const order = { HIGH: 0, MEDIUM: 1, LOW: 2 };
    for (const est of estimates) {
      const worst = Math.max(...est.items.map((i) => order[i.confidence]));
      expect(order[est.overallConfidence]).toBe(worst);
    }
  });

  it("low-confidence meals name their shakiest item", () => {
    for (const est of estimates.filter((e) => e.overallConfidence === "LOW")) {
      const lowNames = est.items
        .filter((i) => i.confidence === "LOW")
        .map((i) => i.name);
      expect(lowNames).toContain(est.lowestConfidenceItem);
    }
  });

  it("fallback estimates are flagged", () => {
    for (const est of estimates) expect(est.fromFallback).toBe(true);
  });

  it("portion correction recomputes honestly and tightens confidence", () => {
    const est = estimates[0];
    const target = est.items[0];
    const corrected = withPortion(est, target.name, target.grams * 2);
    const item = corrected.items.find((i) => i.name === target.name)!;
    expect(item.grams).toBe(target.grams * 2);
    expect(item.confidence).toBe("HIGH");
    expect(item.kcal).toBeGreaterThan(target.kcal);
    expect(corrected.totalKcal).toBe(
      corrected.items.reduce((s, i) => s + i.kcal, 0),
    );
    expect(corrected.totalLow).toBeLessThan(corrected.totalKcal);
    expect(corrected.totalKcal).toBeLessThan(corrected.totalHigh);
  });
});
