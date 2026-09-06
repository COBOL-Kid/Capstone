import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { SEEDED_VINS } from "../../fixtures/test-data";
import {
  expectToast,
  openRecallCompleteForm,
  vehicleDetailToggle,
  waitForAppReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

const camryUrl = `/vehicles/${SEEDED_VINS.camry}`;

test.describe.configure({ mode: "serial" });

test("REC-001: open recalls section renders seeded data", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await vehicleDetailToggle(page)
    .getByRole("button", { name: "Recalls" })
    .click();
  await expect(page.getByText("Open recalls")).toBeVisible();
  await expect(
    page.getByText("FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP"),
  ).toBeVisible();
  await expect(page.getByText("23V123000")).toBeVisible();
});

test("REC-002: completed recalls section renders seeded data", async ({
  page,
}) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await vehicleDetailToggle(page)
    .getByRole("button", { name: "Recalls" })
    .click();
  await page.getByText("Completed recalls").click();
  await expect(page.getByText("ELECTRICAL SYSTEM:SOFTWARE")).toBeVisible();
});

test("REC-003: open recall detail modal", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await vehicleDetailToggle(page)
    .getByRole("button", { name: "Recalls" })
    .click();
  await page
    .getByRole("button", {
      name: /FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP/,
    })
    .click();

  await expect(page.getByRole("dialog", { name: "Recall" })).toBeVisible();
  const recallDialog = page.getByRole("dialog", { name: "Recall" });
  await expect(recallDialog.getByText("23V123000")).toBeVisible();
  await expect(
    recallDialog.getByText("Dealers will replace the fuel pump"),
  ).toBeVisible();
  await expect(
    recallDialog.getByRole("button", { name: "Mark complete" }),
  ).toBeVisible();
});

test("REC-004: mark recall complete", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await vehicleDetailToggle(page)
    .getByRole("button", { name: "Recalls" })
    .click();
  await page
    .getByRole("button", { name: /FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP/ })
    .click();
  const dialog = await openRecallCompleteForm(page);
  await dialog.getByLabel("Completed date").fill("2026-03-01");
  await dialog.getByLabel("Repair shop (optional)").fill("City Toyota");
  await dialog.getByRole("button", { name: "Confirm complete" }).click();

  await expectToast(page, "Recall record updated.");
  await page.getByText("Completed recalls").click();
  await expect(
    page.getByText("FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP"),
  ).toBeVisible();
});

test("REC-005: mark completed recall incomplete", async ({ page }) => {
  await page.goto(camryUrl);
  await waitForAppReady(page);
  await waitForVehicleDetailSettled(page);

  await vehicleDetailToggle(page)
    .getByRole("button", { name: "Recalls" })
    .click();
  await page.getByText("Completed recalls").click();
  await page
    .getByRole("button", { name: /FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP/ })
    .click();
  await page.getByRole("button", { name: "Mark incomplete" }).click();

  await expectToast(page, "Recall record updated.");
  // Exact match: the "No open recalls for this vehicle." empty state also
  // contains "Open recalls" and would make a substring locator ambiguous
  // (strict violation) while the post-mutation refresh is in flight.
  await expect(page.getByText("Open recalls", { exact: true })).toBeVisible();
});

test("REC-007: recall mutation failure shows alert", async ({ page }) => {
  await page.route("**/api/recall/completed", async (route) => {
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

  await vehicleDetailToggle(page)
    .getByRole("button", { name: "Recalls" })
    .click();
  await page
    .getByRole("button", { name: /FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP/ })
    .click();
  const dialog = await openRecallCompleteForm(page);
  await dialog.getByLabel("Completed date").fill("2026-03-02");
  await dialog.getByRole("button", { name: "Confirm complete" }).click();

  await expect(page.getByRole("alert")).toContainText(
    /Unable to mark recall complete/i,
  );
});
