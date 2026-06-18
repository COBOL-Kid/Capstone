import { expect, test } from "@playwright/test";
import { EMPTY_GARAGE_USER } from "../../fixtures/test-data";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test("ERR-003: bootstrap obtains CSRF before logout mutation", async ({
  page,
}) => {
  const csrfRequests: { url: string; time: number }[] = [];
  const logoutRequests: { url: string; time: number }[] = [];

  page.on("request", (request) => {
    const url = request.url();
    const time = Date.now();
    if (url.includes("/api/auth/csrf")) {
      csrfRequests.push({ url, time });
    }
    if (url.includes("/api/auth/logout")) {
      logoutRequests.push({ url, time });
    }
  });

  await page.goto("/");
  await waitForAppReady(page);

  await page.goto("/sign-in");
  await waitForAppReady(page);

  await page.getByLabel("Email").fill(EMPTY_GARAGE_USER.email);
  await page.getByLabel("Password").fill(EMPTY_GARAGE_USER.password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/home$/);

  await page.getByRole("button", { name: "Account" }).click();
  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL("/");

  expect(csrfRequests.length).toBeGreaterThan(0);
  expect(logoutRequests.length).toBe(1);
  expect(csrfRequests[0]!.time).toBeLessThanOrEqual(logoutRequests[0]!.time);
});

test("ERR-004: generic 500 shows friendly message", async ({ page }) => {
  await page.goto("/sign-in");
  await waitForAppReady(page);

  await page.route("**/api/auth/authenticate", async (route) => {
    await route.fulfill({
      status: 500,
      contentType: "text/plain",
      body: "Sometimes things just don't go as planned.",
    });
  });

  await page.getByLabel("Email").fill("test.user@example.com");
  await page.getByLabel("Password").fill("Password123!");
  await page.getByRole("button", { name: "Sign In", exact: true }).click();

  await expect(page.getByRole("alert")).toContainText(
    /Sometimes things just don't go as planned/i,
  );
});
