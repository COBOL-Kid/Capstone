import { expect, test } from "@playwright/test";
import { SEEDED_VINS } from "../../fixtures/test-data";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test("AUTH-004: protected vehicle detail requires auth then loads after sign-in", async ({
  page,
}) => {
  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await expect(page).toHaveURL(/\/sign-in$/);

  await page.getByLabel("Email").fill("test.user@example.com");
  await page.getByLabel("Password").fill("Password123!");
  await page.getByRole("button", { name: "Sign In", exact: true }).click();

  await expect(page).toHaveURL(/\/home$/);
  await waitForAppReady(page);

  await page.goto(`/vehicles/${SEEDED_VINS.camry}`);
  await waitForAppReady(page);
  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry", level: 1 }),
  ).toBeVisible();
});
