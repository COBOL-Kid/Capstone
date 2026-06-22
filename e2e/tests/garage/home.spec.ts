import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS, VEHICLE_LABELS } from "../../fixtures/test-data";
import {
  navigateToVehicleDetail,
  openAddVehicleModal,
  submitAddVehicleForm,
  waitForAppReady,
  waitForGarageReady,
} from "../../fixtures/ui.helpers";

test.describe("Garage / home page", () => {
  test("GAR-001: authenticated garage shows seeded vehicles", async ({
    page,
  }) => {
    await page.goto("/home");
    await waitForAppReady(page);

    await expect(
      page.getByRole("heading", { name: "My Vehicles" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Add new vehicle" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: VEHICLE_LABELS.civic }),
    ).toBeVisible();
    await expect(page.getByText("45200 miles")).toBeVisible();
    await expect(page.getByText("78500 miles")).toBeVisible();

    const camryCard = page.locator("article.vehicle-card", {
      has: page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
    });
    const civicCard = page.locator("article.vehicle-card", {
      has: page.getByRole("heading", { name: VEHICLE_LABELS.civic }),
    });
    await expect(camryCard.getByText("SE")).toBeVisible();
    await expect(civicCard.getByText("EX")).toBeVisible();
    await expect(camryCard.locator("img.vehicle-card__image")).toBeVisible();
    await expect(civicCard.locator("img.vehicle-card__image")).toBeVisible();
  });

  test("GAR-003: non-empty garage shows header add button", async ({
    page,
  }) => {
    await page.goto("/home");
    await waitForAppReady(page);

    await expect(
      page.getByRole("button", { name: "Add new vehicle" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Add a vehicle" }),
    ).toHaveCount(0);
  });

  test("GAR-004: vehicle card navigates to detail page", async ({ page }) => {
    await page.goto("/home");
    await waitForAppReady(page);

    await navigateToVehicleDetail(page, VEHICLE_LABELS.camry);
    await expect(page).toHaveURL(new RegExp(`/vehicles/${SEEDED_VINS.camry}$`));
    await expect(page.getByText("45,200 mi")).toBeVisible();
  });

  test("GAR-005: delete vehicle cancel path keeps vehicle", async ({
    page,
  }) => {
    await page.goto("/home");
    await waitForAppReady(page);

    const card = page.locator("article.vehicle-card", {
      has: page.getByRole("heading", { name: VEHICLE_LABELS.civic }),
    });
    await card.getByRole("button", { name: "Delete vehicle" }).click();
    await expect(
      page.getByRole("dialog", { name: "Delete Vehicle" }),
    ).toBeVisible();

    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(
      page.getByRole("heading", { name: VEHICLE_LABELS.civic }),
    ).toBeVisible();
  });

  test("GAR-008: vehicles API 204 normalizes to empty state", async ({
    page,
  }) => {
    await page.route("**/api/vin", async (route) => {
      if (route.request().method() === "GET") {
        await route.fulfill({ status: 204, body: "" });
        return;
      }
      await route.continue();
    });

    await page.goto("/home");
    await waitForAppReady(page);

    await expect(page.getByText("No vehicles yet")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Add a vehicle" }),
    ).toBeVisible();
  });

  test("GAR-009: garage load failure shows error", async ({ page }) => {
    await page.route("**/api/vin", async (route) => {
      if (route.request().method() === "GET") {
        await route.fulfill({
          status: 500,
          contentType: "text/plain",
          body: "Sometimes things just don't go as planned.",
        });
        return;
      }
      await route.continue();
    });

    await page.goto("/home");
    await waitForAppReady(page);

    await expect(page.getByText("Failed to load vehicles.")).toBeVisible();
  });
});

test.describe("Add vehicle modal", () => {
  test("VIN-001: add vehicle modal opens from header", async ({ page }) => {
    await page.goto("/home");
    await waitForGarageReady(page);

    await openAddVehicleModal(page);
    await expect(page.getByLabel("VIN")).toBeVisible();
    await expect(page.getByLabel("Current Mileage")).toBeVisible();
  });

  test("VIN-002: add vehicle form validation", async ({ page }) => {
    await page.goto("/home");
    await waitForGarageReady(page);
    await openAddVehicleModal(page);

    await submitAddVehicleForm(page);
    await expect(page.getByText("VIN is required.")).toBeVisible();

    await page.getByLabel("VIN").fill("SHORT");
    await page.getByLabel("Current Mileage").fill("-1");
    await submitAddVehicleForm(page);

    await expect(
      page.getByText("VIN must be 17 characters and cannot contain I, O, or Q"),
    ).toBeVisible();
    await expect(page.getByText("Mileage cannot be negative.")).toBeVisible();
  });
});
