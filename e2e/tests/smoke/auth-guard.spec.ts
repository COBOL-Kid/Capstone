import { expect, test } from "@playwright/test";

test("unauthenticated home visit redirects to sign-in", async ({ page }) => {
  await page.goto("/home");

  await expect(page).toHaveURL(/\/sign-in$/);
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Sign In" })).toBeVisible();
});
