import { defineConfig, devices } from "@playwright/test";

const baseURL = "http://localhost:4200";

const authenticatedTestMatch = [
  /\/garage\/home\.spec\.ts/,
  /\/vehicle-detail\//,
  /\/vehicle-onboarding\//,
  /\/account\//,
  /\/maintenance\//,
  /\/recalls\//,
  /\/auth\/(logout|session)\.spec\.ts/,
  /\/errors\/resilience\.spec\.ts/,
];

const authenticatedTestIgnore = [/not-found\.spec\.ts/];

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,
  reporter: [["list"], ["html", { open: "never" }]],
  timeout: 45_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  globalSetup: "./support/global-setup.ts",
  projects: [
    {
      name: "chromium",
      testIgnore: [
        ...authenticatedTestMatch,
        /unverified\.spec\.ts/,
        /empty-garage\.spec\.ts/,
      ],
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "chromium-authenticated",
      testMatch: authenticatedTestMatch,
      testIgnore: [
        /unverified\.spec\.ts/,
        /empty-garage\.spec\.ts/,
        ...authenticatedTestIgnore,
      ],
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "chromium-empty-garage",
      testMatch: [
        /empty-garage\.spec\.ts/,
        /vehicle-detail\/not-found\.spec\.ts/,
      ],
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "chromium-unverified",
      testMatch: /unverified\.spec\.ts/,
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
