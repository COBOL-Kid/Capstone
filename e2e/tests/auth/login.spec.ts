import { expect, test } from "@playwright/test";
import { TEST_USER } from "../../fixtures/test-data";

test("sign in via UI reaches the garage", async ({ page }) => {
  await page.goto("/sign-in");

  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByLabel("Email").fill(TEST_USER.email);
  await page.getByLabel("Password").fill(TEST_USER.password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();

  await expect(page).toHaveURL(/\/home$/);
  await expect(
    page.getByRole("heading", { name: "My Vehicles" }),
  ).toBeVisible();
});
