import { expect, test } from "@playwright/test";
import { deleteUserByEmail } from "../../fixtures/db.helpers";
import { VERIFICATION_CODE } from "../../fixtures/test-data";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test.describe.configure({ mode: "serial" });

test("AUTH-010: successful sign-up enters unverified account flow", async ({
  page,
}) => {
  const email = `e2e-signup-${Date.now()}@example.com`;

  try {
    await page.goto("/sign-up");
    await waitForAppReady(page);

    await page.getByLabel("First name").fill("E2E");
    await page.getByLabel("Last name").fill("Signup");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password").fill("Password123!");
    await page.getByRole("button", { name: "Sign Up", exact: true }).click();

    await expect(page).toHaveURL(/\/home$/);
    await expect(page.getByText("Verify your email")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Add a vehicle" }),
    ).toBeDisabled();
  } finally {
    deleteUserByEmail(email);
  }
});

test("AUTH-014: resend email verification disables controls while pending", async ({
  page,
}) => {
  const email = `e2e-resend-${Date.now()}@example.com`;

  try {
    await page.route("**/api/auth/email-verification/resend", async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 5_000));
      await route.continue();
    });

    await page.goto("/sign-up");
    await waitForAppReady(page);

    await page.getByLabel("First name").fill("E2E");
    await page.getByLabel("Last name").fill("Resend");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password").fill("Password123!");
    await page.getByRole("button", { name: "Sign Up", exact: true }).click();
    await expect(page).toHaveURL(/\/home$/);

    await page.getByRole("button", { name: "Enter code" }).click();
    const resendButton = page.getByRole("button", { name: "Resend code" });
    await resendButton.click();
    await expect(page.getByRole("button", { name: "Sending…" })).toBeVisible({
      timeout: 5_000,
    });

    await page.getByLabel("Verification code").fill(VERIFICATION_CODE);
    await page.getByRole("button", { name: "Verify email" }).click();
    await expect(page.getByText("Verify your email")).toHaveCount(0);
  } finally {
    deleteUserByEmail(email);
  }
});
