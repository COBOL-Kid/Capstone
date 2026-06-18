import { expect, test } from "@playwright/test";
import { loginViaApi } from "../../fixtures/auth.fixture";
import { SEEDED_VINS, TEST_USER } from "../../fixtures/test-data";
import {
  assertNoAuthTokensInWebStorage,
  signInViaUI,
  waitForAppReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

test("AUTH-006: cold-load session hydration works from stored cookies", async ({
  browser,
}) => {
  const setupContext = await browser.newContext();
  const setupPage = await setupContext.newPage();
  await signInViaUI(setupPage);
  const storageState = await setupContext.storageState();
  await setupContext.close();

  const hydratedContext = await browser.newContext({ storageState });
  const hydratedPage = await hydratedContext.newPage();
  await hydratedPage.goto("/home");
  await waitForAppReady(hydratedPage);

  await expect(
    hydratedPage.getByRole("heading", { name: "My Vehicles" }),
  ).toBeVisible();
  await expect(
    hydratedPage.getByRole("heading", { name: "2020 Toyota Camry" }),
  ).toBeVisible();

  await hydratedContext.close();
});

test("ADD-001: auth tokens are not stored in web storage after login", async ({
  page,
}) => {
  await signInViaUI(page);
  await assertNoAuthTokensInWebStorage(page);
});

test("ADD-003: cold deep link to vehicle detail hydrates session", async ({
  browser,
}) => {
  const setupContext = await browser.newContext();
  await loginViaApi(setupContext.request, TEST_USER);
  const storageState = await setupContext.storageState();
  await setupContext.close();

  const hydratedContext = await browser.newContext({ storageState });
  const page = await hydratedContext.newPage();
  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry", level: 1 }),
  ).toBeVisible();

  await hydratedContext.close();
});
