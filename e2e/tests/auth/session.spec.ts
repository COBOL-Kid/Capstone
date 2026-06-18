import { expect, test } from "@playwright/test";
import { signInViaUI, waitForAppReady } from "../../fixtures/ui.helpers";

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
