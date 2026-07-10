import { describe, expect, it } from "vitest";
import { governText, isDistress, SUPPORT_MESSAGE } from "../src/domain/safetyGuard";

// Keep these lists in sync with judgemycal-agent/tests/test_safety.py.
const DISTRESS = [
  "I feel like I should just stop eating",
  "I've been starving myself all week",
  "haven't eaten since Tuesday",
  "I'm skipping meals to hit my goal",
  "I made myself sick after dinner",
  "I hate my body",
  "I'm so fat and worthless",
  "I want to disappear",
  "maybe I should punish myself with a workout",
];

const BENIGN = [
  "what's in this meal?",
  "I had chicken and rice for lunch",
  "build me a 30 minute strength workout",
  "swap suggestions for my shopping basket please",
  "how many calories in a banana",
  "I ate a huge burger and fries today, oops",
  "no oil",
  "",
];

describe("safetyGuard", () => {
  it("detects distress signals", () => {
    for (const text of DISTRESS) expect(isDistress(text), text).toBe(true);
  });

  it("does not flag benign text", () => {
    for (const text of BENIGN) expect(isDistress(text), text).toBe(false);
  });

  it("null/undefined are safe", () => {
    expect(isDistress(null)).toBe(false);
    expect(isDistress(undefined)).toBe(false);
  });

  it("neutralises punitive terms", () => {
    const out = governText("Don't be lazy, that plate is disgusting.");
    expect(out.toLowerCase()).not.toContain("lazy");
    expect(out.toLowerCase()).not.toContain("disgusting");
  });

  it("leaves clean text alone", () => {
    const text = "Nice plate! ~640 kcal (540–750). You logged it, that's the win.";
    expect(governText(text)).toBe(text);
  });

  it("support message steps out of character", () => {
    expect(SUPPORT_MESSAGE).toContain("step out of character");
  });
});
