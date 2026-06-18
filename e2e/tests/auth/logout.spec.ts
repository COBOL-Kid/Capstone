import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS } from "../../fixtures/test-data";
import { signOutViaUI, waitForAppReady } from "../../fixtures/ui.helpers";

test("AUTH-005: logout clears session and garage state", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);
  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry" }),
  ).toBeVisible();

  await signOutViaUI(page);

  await page.goto("/home");
  await expect(page).toHaveURL(/\/sign-in$/);
  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry" }),
  ).toHaveCount(0);
});

test("AUTH-007: mutating requests succeed after bootstrap", async ({
  page,
}) => {
  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await waitForAppReady(page);

  await page.getByRole("button", { name: "Update" }).click();
  await expect(
    page.getByRole("dialog", { name: "Update Mileage" }),
  ).toBeVisible();
  await page.getByLabel("Current mileage").fill("45201");
  await page.getByRole("button", { name: "Save mileage" }).click();

  await expect(page.locator(".hc-toast--success")).toContainText(
    "Mileage updated.",
  );

  await page.getByRole("button", { name: "Update" }).click();
  await page.getByLabel("Current mileage").fill("45200");
  await page.getByRole("button", { name: "Save mileage" }).click();
  await expect(page.locator(".hc-toast--success")).toContainText(
    "Mileage updated.",
  );
});
