import type { Page } from "@playwright/test";
import { SEEDED_VINS } from "./test-data";

const camryDashboardRoute = `**/api/vin/${SEEDED_VINS.camry}/dashboard`;

export async function mockCamryDashboard(
  page: Page,
  overrides: Record<string, unknown> = {},
): Promise<void> {
  await page.route(camryDashboardRoute, async (route) => {
    if (route.request().method() !== "GET") {
      await route.continue();
      return;
    }

    const response = await route.fetch();
    const body = (await response.json()) as Record<string, unknown>;
    const detailOverride = (overrides.detail as Record<string, unknown>) ?? {};

    await route.fulfill({
      response,
      json: {
        ...body,
        ...overrides,
        detail: {
          ...(body.detail as Record<string, unknown>),
          ...detailOverride,
        },
      },
    });
  });
}
