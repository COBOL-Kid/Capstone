import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import {
  buildListingsMockResponse,
  buildMultiPriceListingsMockResponse,
  camryListingsUrl,
  listingsRequestHasPageParam,
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
  test("LIST-001: opens modal and loads listings with pricing summary", async ({
    page,
  }) => {
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
    await expect(dialog.getByText("Low")).toBeVisible();
    await expect(dialog.getByText("Average")).toBeVisible();
    await expect(dialog.getByText("High")).toBeVisible();
    await expect(dialog.getByText("$179,148").first()).toBeVisible();
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
          buildListingsMockResponse({
            total: 0,
            pricingSummary: {
              minPrice: null,
              maxPrice: null,
              averagePrice: null,
              pricedListingCount: 0,
            },
            listings: [],
          }),
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

  test("LIST-004: listings list is scrollable", async ({ page }) => {
    const manyListings = Array.from({ length: 12 }, (_, index) => ({
      ...SAMPLE_LISTING,
      vin: `VIN${String(index).padStart(14, "0")}`,
      price: 20000 + index * 1000,
    }));

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
            total: manyListings.length,
            pricingSummary: {
              minPrice: 20000,
              maxPrice: 31000,
              averagePrice: 25500,
              pricedListingCount: manyListings.length,
            },
            listings: manyListings,
          }),
        ),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    const list = dialog.locator(".vehicle-listings-modal__list");
    await expect(list).toBeVisible();

    const scrollMetrics = await list.evaluate((element) => ({
      scrollHeight: element.scrollHeight,
      clientHeight: element.clientHeight,
    }));
    expect(scrollMetrics.scrollHeight).toBeGreaterThan(
      scrollMetrics.clientHeight,
    );
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

  test("LIST-007: requests listings without page query param", async ({
    page,
  }) => {
    let capturedUrl = "";

    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      capturedUrl = route.request().url();
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(buildListingsMockResponse()),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);
    await openListingsModal(page);

    expect(capturedUrl).toContain(`/api/vin/${SEEDED_VINS.camry}/listings`);
    expect(listingsRequestHasPageParam(capturedUrl)).toBe(false);
  });

  test("LIST-008: text-only rows without pagination controls", async ({
    page,
  }) => {
    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(buildMultiPriceListingsMockResponse(3)),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    await expect(dialog.locator("img")).toHaveCount(0);
    await expect(
      dialog.getByRole("button", { name: "Previous" }),
    ).toHaveCount(0);
    await expect(dialog.getByRole("button", { name: "Next" })).toHaveCount(0);
    await expect(dialog.getByText(/^Page \d+$/)).toHaveCount(0);
    await expect(
      dialog.getByRole("button", { name: "Carfax" }),
    ).toHaveCount(0);
  });

  test("LIST-009: pricing summary shows distinct low average and high values", async ({
    page,
  }) => {
    await page.route(camryListingsUrl(), async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(buildMultiPriceListingsMockResponse(3)),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    const pricing = dialog.locator(".vehicle-listings-modal__pricing");
    await expect(pricing.getByText("$24,500")).toBeVisible();
    await expect(pricing.getByText("$27,000")).toBeVisible();
    await expect(pricing.getByText("$29,500")).toBeVisible();
    await expect(
      dialog.getByText("Based on 3 listings with prices"),
    ).toBeVisible();
  });

  test("LIST-010: listings without prices hide pricing summary", async ({
    page,
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
            listings: [{ ...SAMPLE_LISTING, price: null }],
            pricingSummary: {
              minPrice: null,
              maxPrice: null,
              averagePrice: null,
              pricedListingCount: 0,
            },
          }),
        ),
      });
    });

    await page.goto(camryUrl);
    await waitForAppReady(page);
    await waitForVehicleDetailSettled(page);

    const dialog = await openListingsModal(page);
    await expect(dialog.getByText("2020 Ford Mustang")).toBeVisible();
    await expect(dialog.getByText("Low")).toHaveCount(0);
    await expect(dialog.getByText("Average")).toHaveCount(0);
    await expect(dialog.getByText("High")).toHaveCount(0);
    await expect(
      dialog.getByText(/Based on \d+ listings with prices/),
    ).toHaveCount(0);
  });
});
