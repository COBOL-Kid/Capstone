import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import {
  buildListingsMockResponse,
  camryListingsUrl,
  SAMPLE_LISTING,
} from "../../fixtures/listings.helpers";
import { SEEDED_VINS } from "../../fixtures/test-data";
import {
  listingsDialog,
  openListingsModal,
  waitForAppReady,
  waitForVehicleDetailSettled,
} from "../../fixtures/ui.helpers";

const camryUrl = `/vehicles/${SEEDED_VINS.camry}`;

test.describe("Vehicle listings modal", () => {
  test("LIST-001: opens modal and loads page 1", async ({ page }) => {
    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(buildListingsMockResponse()),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    await expect(dialog.getByText("2020 Toyota Camry")).toBeVisible();
    await expect(
      dialog.getByText("661 comparable listings found"),
    ).toBeVisible();
    await expect(dialog.getByText("2020 Ford Mustang")).toBeVisible();
    await expect(dialog.getByText("Earth Motorcars")).toBeVisible();
    await expect(
      dialog.getByRole("button", { name: "View listing" }),
    ).toBeVisible();
  });

  test("LIST-002: empty listings shows message", async ({ page }) => {
    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          buildListingsMockResponse({ total: 0, listings: [] }),
        ),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    await expect(
      dialog.getByText("No comparable vehicles are listed right now."),
    ).toBeVisible();
  });

  test("LIST-003: rate limit message is shown", async ({ page }) => {
    const rateLimitMessage =
      "Vehicle listings are temporarily unavailable. Please try again shortly.";

    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 429,
        contentType: "text/plain",
        body: rateLimitMessage,
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    await expect(dialog.getByRole("alert")).toContainText(rateLimitMessage);
  });

  test("LIST-004: pagination loads next page", async ({ page }) => {
    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      const url = new URL(route.request().url());
      const pageNum = url.searchParams.get("page") ?? "1";

      if (pageNum === "1") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(buildListingsMockResponse()),
        });
        return;
      }

      if (pageNum === "2") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(
            buildListingsMockResponse({
              page: 2,
              listings: [
                {
                  ...SAMPLE_LISTING,
                  vin: "SECONDLISTINGVIN12",
                  year: "2019",
                  make: "Honda",
                  model: "Civic",
                },
              ],
            }),
          ),
        });
        return;
      }

      await route.continue();
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    await expect(dialog.getByText("2020 Ford Mustang")).toBeVisible();

    await dialog.getByRole("button", { name: "Next" }).click();
    await expect(dialog.getByText("Page 2")).toBeVisible();
    await expect(dialog.getByText("2019 Honda Civic")).toBeVisible();
  });

  test("LIST-005: close blocked while loading", async ({ page }) => {
    let resolveFirstResponse: (() => void) | undefined;
    const firstResponseGate = new Promise<void>((resolve) => {
      resolveFirstResponse = resolve;
    });

    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await firstResponseGate;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(buildListingsMockResponse()),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    await page.getByRole("button", { name: "Current Market Value" }).click();
    const dialog = listingsDialog(page);
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText("Loading listings…")).toBeVisible();

    await page.keyboard.press("Escape");
    await expect(dialog).toBeVisible();

    const closeButton = dialog.getByRole("button", {
      name: "Close current market value dialog",
    });
    await expect(closeButton).toBeDisabled();

    resolveFirstResponse!();
    await expect(dialog.getByText("661 comparable listings found")).toBeVisible();

    await dialog
      .getByRole("button", { name: "Close current market value dialog" })
      .click();
    await expect(dialog).toBeHidden();
  });

  test("LIST-006: view listing opens external URL", async ({
    page,
    context,
  }) => {
    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          buildListingsMockResponse({
            listings: [
              {
                ...SAMPLE_LISTING,
                vdp: "https://example.com/listing/camry-comparable",
              },
            ],
          }),
        ),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    const popupPromise = context.waitForEvent("page");
    await dialog.getByRole("button", { name: "View listing" }).click();
    const popup = await popupPromise;
    await expect(popup).toHaveURL(/example\.com\/listing\/camry-comparable/);
    await popup.close();
  });
});
