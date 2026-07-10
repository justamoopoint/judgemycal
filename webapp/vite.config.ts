import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  test: {
    // Playwright owns e2e/; vitest owns tests/.
    include: ["tests/**/*.test.ts"],
  },
});
