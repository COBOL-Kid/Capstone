import { expect, test } from "@playwright/test";

test("garage shows seeded vehicles for authenticated user", async ({
  page,
}) => {
  await page.goto("/home");

  await expect(
    page.getByRole("heading", { name: "My Vehicles" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add new vehicle" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "2018 Honda Civic" }),
  ).toBeVisible();
});
