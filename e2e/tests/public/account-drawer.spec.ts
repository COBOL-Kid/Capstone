import { expect, test } from "@playwright/test";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test("ACCT-002: signed-out account drawer shows sign-in actions", async ({
  page,
}) => {
  await page.goto("/");
  await waitForAppReady(page);

  await page.getByRole("button", { name: "Account" }).click();
  await expect(
    page.getByText("Sign in to view your account details."),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign In" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign Up" })).toBeVisible();
});
