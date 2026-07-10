import { expect, test } from "@playwright/test";

// These run against the production build in offline demo mode (no backend
// configured), exercising the in-browser fallback engines end to end.

test("demo meal: honest range shown, logged meal lands in Today", async ({ page }) => {
  await page.goto("/");
  await page.getByTestId("nav_capture").click();
  await page.getByTestId("demo_meal").click();

  // The estimate is a RANGE with a persona reaction.
  await expect(page.getByTestId("estimate_range")).toBeVisible();
  await expect(page.getByTestId("estimate_range")).toContainText("–");
  await expect(page.getByTestId("persona_reaction")).toBeVisible();
  await expect(page.getByTestId("offline_badge")).toBeVisible();

  await page.getByTestId("log_meal").click();
  await page.getByTestId("nav_home").click();
  await expect(page.getByTestId("home_total")).toBeVisible();
});

test("portion correction recomputes the range", async ({ page }) => {
  await page.goto("/");
  await page.getByTestId("nav_capture").click();
  await page.getByTestId("demo_meal").click();
  await expect(page.getByTestId("estimate_range")).toBeVisible();

  const before = await page.getByTestId("estimate_total").textContent();
  await page.locator('[data-testid^="portion_"]').first().click();
  await expect(page.getByTestId("estimate_range")).toBeVisible();
  const after = await page.getByTestId("estimate_total").textContent();
  expect(after).not.toBe(before);
});

test("safety floor: distress note breaks character and nothing leaves the page", async ({
  page,
}) => {
  const requests: string[] = [];
  await page.route("**/*", (route) => {
    const url = route.request().url();
    if (!url.startsWith("http://127.0.0.1:4173")) requests.push(url);
    return route.continue();
  });

  await page.goto("/");
  await page.getByTestId("nav_capture").click();
  await page
    .getByTestId("note_input")
    .fill("I feel like I should just stop eating");
  await page.getByTestId("demo_meal").click();

  // The support message owns the screen…
  await expect(page.getByTestId("support_message")).toBeVisible();
  await expect(page.getByTestId("support_message")).toContainText(
    "step out of character",
  );
  // …no estimate was produced, and no request left the page.
  await expect(page.getByTestId("estimate_range")).toHaveCount(0);
  expect(requests).toEqual([]);

  await page.getByTestId("support_dismiss").click();
});

test("distress basket item also breaks character", async ({ page }) => {
  await page.goto("/");
  await page.getByTestId("nav_shopping").click();
  await page.getByTestId("basket_input").fill("I hate my body");
  await page.getByTestId("basket_add").click();
  await expect(page.getByTestId("support_message")).toBeVisible();
});

test("persona switching: Buddy default, Auntie opt-in", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByTestId("persona_switcher")).toContainText("Buddy");
  await page.getByTestId("persona_switcher").click();
  await page.getByTestId("persona_auntie").click();
  await expect(page.getByTestId("persona_switcher")).toContainText("Auntie");
});
