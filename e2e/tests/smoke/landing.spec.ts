import { expect, test } from "@playwright/test";

test("landing page shows sign-in entry point", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("link", { name: "Sign In" })).toBeVisible();
});
