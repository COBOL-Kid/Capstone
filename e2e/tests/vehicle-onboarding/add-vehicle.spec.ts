import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import {
  submitAddVehicleForm,
  waitForAppReady,
} from "../../fixtures/ui.helpers";

test("VIN-005: VIN not found shows friendly message", async ({ page }) => {
  await page.route("**/api/vin", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 404,
        contentType: "text/plain",
        body: "We don't have vehicle information for this VIN in our system yet.",
      });
      return;
    }
    await route.continue();
  });

  await page.goto("/home");
  await waitForAppReady(page);
  await page.getByRole("button", { name: "Add new vehicle" }).click();

  await page.getByLabel("VIN").fill("1HGBH41JXMN109186");
  await page.getByLabel("Current Mileage").fill("12000");
  await submitAddVehicleForm(page);

  await expect(page.getByRole("alert")).toContainText(
    /don't have vehicle information/i,
  );
});

test("VIN-006: onboarding backend failure keeps modal open", async ({
  page,
}) => {
  await page.route("**/api/vin", async (route) => {
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

  await page.goto("/home");
  await waitForAppReady(page);
  await page.getByRole("button", { name: "Add new vehicle" }).click();

  await page.getByLabel("VIN").fill("1HGBH41JXMN109186");
  await page.getByLabel("Current Mileage").fill("12000");
  await submitAddVehicleForm(page);

  await expect(page.getByRole("alert")).toContainText(
    /couldn't load vehicle data/i,
  );
  await expect(
    page.getByRole("dialog", { name: "Add a vehicle" }),
  ).toBeVisible();
  await expect(
    page
      .getByRole("dialog", { name: "Add a vehicle" })
      .getByRole("button", { name: "Add Vehicle", exact: true }),
  ).toBeEnabled();
});

test("VIN-007: trim selection required flow", async ({ page }) => {
  let postCount = 0;
  await page.route("**/api/vin**", async (route) => {
    const url = route.request().url();
    if (url.includes("/trim-options")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ trims: ["LX", "EX"] }),
      });
      return;
    }

    if (route.request().method() === "POST") {
      postCount += 1;
      const body = route.request().postDataJSON() as { selectedTrim?: string };
      if (!body.selectedTrim) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            requiresTrimSelection: true,
            year: "2020",
            make: "Honda",
            model: "Accord",
          }),
        });
        return;
      }

      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({
          vin: "1HGBH41JXMN109186",
          currentMileage: 12000,
          vehicleTypeId: 99,
          make: "Honda",
          model: "Accord",
          trim: "EX",
          year: "2020",
          availableImageUrls: [],
          selectedImageUrl: "",
          createdVin: true,
          createdAssociation: true,
        }),
      });
      return;
    }

    await route.continue();
  });

  await page.goto("/home");
  await waitForAppReady(page);
  await page.getByRole("button", { name: "Add new vehicle" }).click();

  await page.getByLabel("VIN").fill("1HGBH41JXMN109186");
  await page.getByLabel("Current Mileage").fill("12000");
  await submitAddVehicleForm(page);

  await page.getByRole("button", { name: "Continue anyways" }).click();
  await expect(page.locator("#add-vehicle-trim")).toBeVisible();
  await page.locator("#add-vehicle-trim").selectOption("EX");
  await page.getByRole("button", { name: "Confirm" }).click();

  expect(postCount).toBeGreaterThanOrEqual(2);
  await expect(page.locator(".hc-toast--success")).toContainText(
    "Vehicle added to your garage.",
  );
  await expect(page).toHaveURL(/\/vehicles\/1HGBH41JXMN109186$/);
});
