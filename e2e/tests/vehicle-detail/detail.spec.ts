import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS, VEHICLE_LABELS } from "../../fixtures/test-data";
import {
  expectToast,
  selectVehiclePhoto,
  vehicleDetailToggle,
  waitForAppReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

const camryUrl = `/vehicles/${SEEDED_VINS.camry}`;

test.describe("Vehicle detail page", () => {
  test("VEH-001: seeded vehicle detail loads", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await expect(
      page.getByRole("heading", { name: VEHICLE_LABELS.camry, level: 1 }),
    ).toBeVisible();
    await expect(page.getByText(SEEDED_VINS.camry)).toBeVisible();
    await expect(page.getByText("45,200 mi")).toBeVisible();
    await expect(page.getByText("Trim")).toBeVisible();
    await expect(page.getByText("4-Door Sedan")).toBeVisible();
  });

  test("VEH-002: back to vehicles link returns to garage", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await page.getByRole("link", { name: /Back to vehicles/i }).click();
    await expect(page).toHaveURL(/\/home$/);
    await expect(
      page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
    ).toBeVisible();
  });

  test("VEH-005: maintenance and recalls toggle", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await expect(page.getByText("Upcoming maintenance")).toBeVisible();
    await vehicleDetailToggle(page)
      .getByRole("button", { name: "Recalls" })
      .click();
    await expect(page.getByText("Open recalls")).toBeVisible();
    await expect(page.getByText("23V123000")).toBeVisible();

    await vehicleDetailToggle(page)
      .getByRole("button", { name: "Maintenance" })
      .click();
    await expect(page.getByText("Upcoming maintenance")).toBeVisible();
  });

  test("VEH-006: owner manual opens external URL", async ({
    page,
    context,
  }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const popupPromise = context.waitForEvent("page");
    await page.getByRole("button", { name: "Owner's manual" }).click();
    const popup = await popupPromise;
    await expect(popup).toHaveURL(/toyota\.com/);
    await popup.close();
  });

  test("VEH-008: warranty information modal", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await page.getByRole("button", { name: "Warranty information" }).click();
    await expect(
      page.getByRole("dialog", { name: "Warranty information" }),
    ).toBeVisible();
    await expect(
      page
        .getByRole("dialog", { name: "Warranty information" })
        .getByText("Basic"),
    ).toBeVisible();
    await page
      .getByRole("dialog", { name: "Warranty information" })
      .getByRole("button", { name: "Close warranty information dialog" })
      .click();
    await expect(
      page.getByRole("dialog", { name: "Warranty information" }),
    ).toBeHidden();
  });

  test("VEH-010: maintenance costs modal", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await page.getByRole("button", { name: "Maintenance costs" }).click();
    const dialog = page.getByRole("dialog", { name: "Maintenance costs" });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText("Oil Change")).toBeVisible();
    await expect(dialog.getByText("Independent shop").first()).toBeVisible();
    await dialog
      .getByRole("button", { name: "Close maintenance costs dialog" })
      .click();
  });

  test("VEH-011: current market value entry point is visible", async ({
    page,
  }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await expect(
      page.getByRole("button", { name: "Current Market Value" }),
    ).toBeVisible();
  });

  test("VEH-012: update mileage happy path", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await page.getByRole("button", { name: "Update" }).click();
    await page.getByLabel("Current mileage").fill("45250");
    await page.getByRole("button", { name: "Save mileage" }).click();

    await expectToast(page, "Mileage updated.");
    await expect(page.getByText("45,250 mi")).toBeVisible();

    await page.getByRole("button", { name: "Update" }).click();
    await page.getByLabel("Current mileage").fill("45200");
    await page.getByRole("button", { name: "Save mileage" }).click();
    await expectToast(page, "Mileage updated.");
  });

  test("VEH-015: vehicle photo modal updates selection", async ({ page }) => {
    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await page.getByRole("button", { name: "Change vehicle photo" }).click();
    await expect(
      page.getByRole("dialog", { name: "Vehicle photos" }),
    ).toBeVisible();

    await selectVehiclePhoto(page, 2).click();
    await expectToast(page, "Vehicle photo updated.");
    await expect(
      page.getByRole("dialog", { name: "Vehicle photos" }),
    ).toBeHidden();

    await page.getByRole("button", { name: "Change vehicle photo" }).click();
    await selectVehiclePhoto(page, 1).click();
    await expectToast(page, "Vehicle photo updated.");
  });

  test("VEH-004: vehicle detail load failure", async ({ page }) => {
    await page.route("**/dashboard", async (route) => {
      if (!route.request().url().includes(SEEDED_VINS.camry)) {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 500,
        contentType: "text/plain",
        body: "Sometimes things just don't go as planned.",
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await expect(page.getByText("Failed to load vehicle.")).toBeVisible({
      timeout: 20_000,
    });
  });
});
