import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test("ERR-005: server unavailable during garage load keeps shell usable", async ({
  page,
}) => {
  await page.route("**/api/vin", async (route) => {
    if (route.request().method() === "GET") {
      await route.abort("failed");
      return;
    }
    await route.continue();
  });

  await page.goto("/home");
  await waitForAppReady(page);

  await expect(page.getByRole("navigation", { name: "Primary" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
  await expect(page.getByText("Failed to load vehicles.")).toBeVisible();
});
