import { defineConfig } from "@playwright/test";
import { existsSync } from "node:fs";

// This dev environment ships Chromium at /opt/pw-browsers/chromium (possibly
// older than this @playwright/test pin); CI runs `npx playwright install
// chromium` and uses the default resolution.
const localChromium = "/opt/pw-browsers/chromium";

export default defineConfig({
  testDir: "e2e",
  timeout: 30_000,
  use: {
    baseURL: "http://127.0.0.1:4173",
    ...(process.env.CI || !existsSync(localChromium)
      ? {}
      : { launchOptions: { executablePath: localChromium } }),
  },
  webServer: {
    command: "npm run preview -- --host 127.0.0.1 --port 4173 --strictPort",
    url: "http://127.0.0.1:4173",
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
