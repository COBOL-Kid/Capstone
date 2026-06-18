import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS } from "../../fixtures/test-data";
import {
  waitForAppReady,
  waitForGarageReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

test("ERR-001: 401 from account hydration clears session", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await page.route("**/api/account/me", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "text/plain",
      body: "Unauthorized",
    });
  });

  await page.reload();
  await waitForAppReady(page);
  await page.goto("/home");
  await expect(page).toHaveURL(/\/sign-in$/);
});

test("ERR-002: 403 account hydration does not wipe session", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  let accountCalls = 0;
  await page.route("**/api/account/me", async (route) => {
    accountCalls += 1;
    if (accountCalls === 1) {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 403,
      contentType: "text/plain",
      body: "Forbidden",
    });
  });

  await page.reload();
  await waitForAppReady(page);
  await page.goto("/home");
  await waitForGarageReady(page);
});

test("ERR-006: browser refresh on protected routes rehydrates", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);
  await expect(
    page.getByRole("heading", { name: "My Vehicles" }),
  ).toBeVisible();

  await page.reload();
  await waitForAppReady(page);
  await expect(
    page.getByRole("heading", { name: "My Vehicles" }),
  ).toBeVisible();
});

test("ERR-006 extension: browser refresh on vehicle detail rehydrates", async ({
  page,
}) => {
  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page.reload();
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);
  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry", level: 1 }),
  ).toBeVisible();
});
