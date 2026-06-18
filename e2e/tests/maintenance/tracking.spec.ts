import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS } from "../../fixtures/test-data";
import {
  expectToast,
  maintenanceDetailDialog,
  openMaintenanceCompleteForm,
  waitForAppReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

const camryUrl = `/vehicles/${SEEDED_VINS.camry}`;

test.describe.configure({ mode: "serial" });

test("MAIN-001: upcoming maintenance section renders seeded data", async ({
  page,
}) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await expect(page.getByText("Upcoming maintenance")).toBeVisible();
  await expect(page.getByText("30,000 mi service")).toBeVisible();
  await expect(page.getByText("Inspect - Transmission fluid")).toBeVisible();
  await expect(page.getByText("45,000 mi service")).toBeVisible();
});

test("MAIN-002: show inspections toggle", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  const inspectionRow = page.getByText("Inspect - Transmission fluid");
  await expect(inspectionRow).toBeVisible();

  await page.locator("label.vehicle-detail__inspect-filter").click();
  await expect(inspectionRow).toHaveCount(0);

  await page.locator("label.vehicle-detail__inspect-filter").click();
  await expect(inspectionRow).toBeVisible();
});

test("MAIN-003: mark upcoming maintenance complete", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page
    .locator(".vehicle-detail__maint-item", {
      hasText: "Replace - Spark plugs",
    })
    .getByRole("button", { name: "Mark complete" })
    .click();
  const dialog = await openMaintenanceCompleteForm(page);
  await dialog.getByLabel("Completed date").fill("2026-01-15");
  await dialog.getByLabel("Mileage completed").fill("45100");
  await dialog.getByLabel("Cost (optional)").fill("150");
  await dialog.getByLabel("Notes (optional)").fill("E2E maintenance test");
  await dialog.getByRole("button", { name: "Confirm complete" }).click();

  await expectToast(page, "Maintenance record updated.");
  await expect(page.getByText("Completed maintenance")).toBeVisible();
});

test("MAIN-004: mark completed maintenance incomplete", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page.getByText("Completed maintenance").click();
  await page.getByRole("button", { name: /Replace - Spark plugs/ }).click();
  await page.getByRole("button", { name: "Mark incomplete" }).click();

  await expectToast(page, "Maintenance record updated.");
  await expect(page.getByText("Replace - Spark plugs")).toBeVisible();
});

test("MAIN-005: completed maintenance detail displays saved fields", async ({
  page,
}) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page.getByText("Completed maintenance").click();
  await page.getByRole("button", { name: /Change - Engine oil/ }).click();

  const dialog = maintenanceDetailDialog(page);
  await expect(dialog.getByText("Change - Engine oil")).toBeVisible();
  await expect(dialog.getByText("2020-06-10 at 5100 miles")).toBeVisible();
  await expect(dialog.getByText("$77.5")).toBeVisible();
  await expect(
    dialog.getByText("First oil change at local shop"),
  ).toBeVisible();
});

test("MAIN-007: maintenance mutation failure shows alert", async ({ page }) => {
  await page.route("**/api/maintenance/completed", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 500,
        contentType: "text/plain",
        body: "Sometimes things just don't go as planned.",
      });
      return;
    }
    await route.continue();
  });

  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await page
    .locator(".vehicle-detail__maint-item", {
      hasText: "Inspect - Transmission fluid",
    })
    .getByRole("button", { name: "Mark complete" })
    .click();
  const dialog = await openMaintenanceCompleteForm(page);
  await dialog.getByLabel("Completed date").fill("2026-02-01");
  await dialog.getByLabel("Mileage completed").fill("30000");
  await dialog.getByRole("button", { name: "Confirm complete" }).click();

  await expect(page.getByRole("alert")).toContainText(
    /Unable to mark maintenance complete/i,
  );
});
