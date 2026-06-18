import { defineConfig, devices } from "@playwright/test";

const baseURL = "http://localhost:4200";

const authenticatedTestMatch = [
  /\/garage\/home\.spec\.ts/,
  /\/vehicle-detail\//,
  /\/vehicle-onboarding\//,
  /\/account\/drawer\.spec\.ts/,
  /\/maintenance\//,
  /\/recalls\//,
  /\/auth\/(logout|session)\.spec\.ts/,
  /\/errors\/resilience\.spec\.ts/,
  /\/errors\/offline\.spec\.ts/,
  /\/errors\/auth-routing\.spec\.ts/,
];

const authenticatedTestIgnore = [/not-found\.spec\.ts/];

const destructiveTestMatch = [
  /\/account\/(password-change|account-change)\.spec\.ts/,
  /\/auth\/signup\.spec\.ts/,
];

export default defineConfig({
  testDir: "./tests",
  // Serial execution: authenticated fixtures share seeded users; password login
  // revokes refresh tokens (deleteByUser), so parallel workers invalidate each other.
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: 1,
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
        ...destructiveTestMatch,
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
        ...destructiveTestMatch,
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
    {
      name: "chromium-destructive",
      testMatch: destructiveTestMatch,
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
