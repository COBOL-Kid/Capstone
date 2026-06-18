import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { mockCamryDashboard } from "../../fixtures/dashboard-mock.helpers";
import { SEEDED_VINS } from "../../fixtures/test-data";
import {
  selectVehiclePhoto,
  waitForAppReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

const camryUrl = `/vehicles/${SEEDED_VINS.camry}`;

test("VEH-007: owner manual unavailable disables button", async ({
  page,
  context,
}) => {
  await mockCamryDashboard(page, {
    detail: { ownersManual: null },
  });

  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  const manualButton = page.getByRole("button", { name: "Owner's manual" });
  await expect(manualButton).toBeDisabled();

  const popupPromise = context
    .waitForEvent("page", { timeout: 1_000 })
    .catch(() => null);
  await manualButton.click({ force: true });
  expect(await popupPromise).toBeNull();
});

test("VEH-009: warranty unavailable disables button", async ({ page }) => {
  await mockCamryDashboard(page, { vehicleWarranty: null });

  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await expect(
    page.getByRole("button", { name: "Warranty information" }),
  ).toBeDisabled();
});

test("VEH-011: maintenance costs unavailable disables button", async ({
  page,
}) => {
  await mockCamryDashboard(page, { miscMaintenanceCosts: [] });

  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await expect(
    page.getByRole("button", { name: "Maintenance costs" }),
  ).toBeDisabled();
});

test("VEH-013: update mileage validation and server failure", async ({
  page,
}) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page.getByRole("button", { name: "Update" }).click();
  await page.getByLabel("Current mileage").fill("");
  await page.getByRole("button", { name: "Save mileage" }).click();
  await expect(
    page.getByRole("dialog", { name: "Update Mileage" }),
  ).toBeVisible();

  await page.getByLabel("Current mileage").fill("46000");
  await page.route(`**/api/vin/${SEEDED_VINS.camry}/mileage`, async (route) => {
    if (route.request().method() === "PATCH") {
      await route.fulfill({
        status: 500,
        contentType: "text/plain",
        body: "Sometimes things just don't go as planned.",
      });
      return;
    }
    await route.continue();
  });
  await page.getByRole("button", { name: "Save mileage" }).click();
  await expect(page.getByRole("alert")).toContainText(
    /Unable to update mileage/i,
  );
  await expect(
    page.getByRole("dialog", { name: "Update Mileage" }),
  ).toBeVisible();
});

test("VEH-014: cannot close mileage modal while saving", async ({ page }) => {
  await page.route(`**/api/vin/${SEEDED_VINS.camry}/mileage`, async (route) => {
    if (route.request().method() === "PATCH") {
      await new Promise((resolve) => setTimeout(resolve, 2_000));
      await route.continue();
      return;
    }
    await route.continue();
  });

  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page.getByRole("button", { name: "Update" }).click();
  await page.getByLabel("Current mileage").fill("45210");
  await page.getByRole("button", { name: "Save mileage" }).click();

  const closeButton = page.getByRole("button", {
    name: "Close update mileage dialog",
  });
  await expect(closeButton).toBeDisabled();
  await page.keyboard.press("Escape");
  await expect(
    page.getByRole("dialog", { name: "Update Mileage" }),
  ).toBeVisible();
});

test("VEH-016: vehicle photo server failure keeps modal open", async ({
  page,
}) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page.route(`**/api/vin/${SEEDED_VINS.camry}/photo`, async (route) => {
    if (route.request().method() === "PATCH") {
      await route.fulfill({
        status: 500,
        contentType: "text/plain",
        body: "Sometimes things just don't go as planned.",
      });
      return;
    }
    await route.continue();
  });

  await page.getByRole("button", { name: "Change vehicle photo" }).click();
  await selectVehiclePhoto(page, 2).click();

  await expect(page.getByRole("alert")).toBeVisible();
  await expect(
    page.getByRole("dialog", { name: "Vehicle photos" }),
  ).toBeVisible();
});
