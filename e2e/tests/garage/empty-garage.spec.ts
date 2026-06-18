import {
  emptyGarageTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import {
  linkSeededCamryToEmptyGarageUser,
  unlinkCamryFromEmptyGarageUser,
} from "../../fixtures/db.helpers";
import { SEEDED_VINS, VEHICLE_LABELS } from "../../fixtures/test-data";
import {
  expectToast,
  openAddVehicleModal,
  submitAddVehicleForm,
  waitForAppReady,
} from "../../fixtures/ui.helpers";

test.describe.configure({ mode: "serial" });

test.beforeAll(() => {
  unlinkCamryFromEmptyGarageUser();
});

test("GAR-002: empty garage state shows primary add button", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await expect(page.getByText("No vehicles yet")).toBeVisible();
  await expect(
    page.getByText("Add your first vehicle to track maintenance"),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add a vehicle" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add new vehicle" }),
  ).toHaveCount(0);
});

test("VIN-001: add vehicle modal opens from empty garage", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAddVehicleModal(page);
  await expect(
    page.getByRole("dialog", { name: "Add a vehicle" }),
  ).toBeVisible();
});

test("VIN-003: add existing seeded VIN completes onboarding", async ({
  page,
}) => {
  await page.route("**/api/vin**", async (route) => {
    const url = route.request().url();
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({
          vin: SEEDED_VINS.camry,
          currentMileage: 46000,
          vehicleTypeId: 1,
          make: "Toyota",
          model: "Camry",
          trim: "SE",
          year: "2020",
          availableImageUrls: [
            "https://images.unsplash.com/photo-1621007947382-bcb49c457f54",
          ],
          selectedImageUrl:
            "https://images.unsplash.com/photo-1621007947382-bcb49c457f54",
          createdVin: false,
          createdAssociation: true,
          createdVehicleType: false,
        }),
      });
      return;
    }

    if (
      url.includes(`/api/vin/${SEEDED_VINS.camry}/dashboard`) &&
      route.request().method() === "GET"
    ) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          detail: {
            vin: SEEDED_VINS.camry,
            vehicleTypeId: 1,
            vehicleMake: "Toyota",
            vehicleModel: "Camry",
            vehicleTrim: "SE",
            vehicleYear: "2020",
            vehicleStyle: "4-Door Sedan",
            sourceVin: SEEDED_VINS.camry,
            origin: null,
            body: "Sedan",
            engineDescription: "2.5L 4-Cylinder",
            transmissionStyle: "Automatic",
            driveType: "FWD",
            ownersManual: "https://www.toyota.com/owners",
            currentMileage: 46000,
            availableImageUrls: [
              "https://images.unsplash.com/photo-1621007947382-bcb49c457f54",
            ],
            selectedImageUrl:
              "https://images.unsplash.com/photo-1621007947382-bcb49c457f54",
          },
          upcomingMaintenance: [],
          completedMaintenance: [],
          uncompletedRecalls: [],
          completedRecalls: [],
          miscMaintenanceCosts: [],
          vehicleWarranty: null,
        }),
      });
      return;
    }

    await route.continue();
  });

  await page.goto("/home");
  await waitForAppReady(page);
  await openAddVehicleModal(page);

  await page.getByLabel("VIN").fill(SEEDED_VINS.camry);
  await page.getByLabel("Current Mileage").fill("46000");
  await submitAddVehicleForm(page);

  await expectToast(page, "Vehicle added to your garage.");
  await expect(page).toHaveURL(new RegExp(`/vehicles/${SEEDED_VINS.camry}$`));
  await expect(
    page.getByRole("heading", { name: VEHICLE_LABELS.camry, level: 1 }),
  ).toBeVisible();
});

test("GAR-007: delete vehicle server failure keeps vehicle", async ({
  page,
}) => {
  linkSeededCamryToEmptyGarageUser();

  await page.route(`**/api/vin/${SEEDED_VINS.camry}`, async (route) => {
    if (route.request().method() === "DELETE") {
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

  const card = page.locator("article.vehicle-card", {
    has: page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
  });
  await card.getByRole("button", { name: "Delete vehicle" }).click();
  await page
    .getByRole("dialog", { name: "Delete Vehicle" })
    .getByRole("button", { name: "Delete Vehicle", exact: true })
    .click();

  await expect(
    page.getByRole("dialog", { name: "Delete Vehicle" }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toContainText(
    /error occurred deleting|Sometimes things just don't go as planned/i,
  );
  await expect(
    page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
  ).toBeVisible();
});

test("GAR-006: delete vehicle happy path removes card", async ({ page }) => {
  linkSeededCamryToEmptyGarageUser();

  await page.goto("/home");
  await waitForAppReady(page);

  const card = page.locator("article.vehicle-card", {
    has: page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
  });
  await card.getByRole("button", { name: "Delete vehicle" }).click();
  await page
    .getByRole("dialog", { name: "Delete Vehicle" })
    .getByRole("button", { name: "Delete Vehicle", exact: true })
    .click();

  await expectToast(page, "Vehicle removed.");
  await expect(
    page.getByRole("heading", { name: VEHICLE_LABELS.camry }),
  ).toHaveCount(0);
  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await expect(page.locator(".vehicle-detail__status--error")).toContainText(
    "Vehicle not found.",
  );
});
