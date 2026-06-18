import {
  emptyGarageTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS } from "../../fixtures/test-data";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test.fixme("VEH-003: not-found vehicle shows message", async ({ page }) => {
  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await waitForAppReady(page);

  await expect(page.locator(".vehicle-detail__status--error")).toContainText(
    "Vehicle not found.",
    { timeout: 20_000 },
  );
});
