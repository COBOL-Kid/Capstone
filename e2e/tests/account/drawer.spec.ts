import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { TEST_USER } from "../../fixtures/test-data";
import {
  closeAccountDrawer,
  openAccountDrawer,
  waitForAppReady,
} from "../../fixtures/ui.helpers";

test("ACCT-001: account drawer opens for signed-in user", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await expect(page.getByText("Signed in as")).toBeVisible();
  await expect(
    page.getByText(`${TEST_USER.firstName} ${TEST_USER.lastName}`),
  ).toBeVisible();
  await expect(page.getByText(TEST_USER.email)).toBeVisible();
  await expect(page.getByText("Member Since")).toBeVisible();
  await expect(page.getByText("Last Updated")).toBeVisible();
  await expect(page.getByRole("button", { name: "Log out" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Change Email" }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Change SMS" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Change Password" }),
  ).toBeVisible();
});

test("ACCT-003: account drawer closes via close button", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await closeAccountDrawer(page);
  await expect(page.getByRole("dialog", { name: "Account" })).toBeHidden();
});

test("ACCT-006: change SMS validation blocks invalid input", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change SMS" }).click();
  await page
    .getByLabel("New SMS phone number")
    .fill("abc!!!toolongphonenumberhere");
  await page.getByRole("button", { name: "Send verification code" }).click();

  await expect(
    page.getByText(
      "Use a phone number with digits and common phone characters.",
    ),
  ).toBeVisible();
});

test("ACCT-008: change email duplicate/invalid errors", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change Email" }).click();
  await page.getByLabel("New email address").fill("not-an-email");
  await page.getByRole("button", { name: "Send verification code" }).click();
  await expect(page.getByText("Enter a valid email address.")).toBeVisible();

  await page.getByLabel("New email address").fill(TEST_USER.email);
  await page.getByRole("button", { name: "Send verification code" }).click();
  await expect(page.getByRole("alert")).toBeVisible();
});

test("ACCT-010: change password validation", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change Password" }).click();
  await page.getByRole("button", { name: "Send verification code" }).click();

  await expect(page.getByText("Current password is required.")).toBeVisible();
  await expect(page.getByText("New password is required.")).toBeVisible();
  await expect(page.getByText("Confirm your new password.")).toBeVisible();
});

test("ACCT-013: account details load failure", async ({ page }) => {
  await page.route("**/api/account/me", async (route) => {
    await route.fulfill({
      status: 500,
      contentType: "text/plain",
      body: "Sometimes things just don't go as planned.",
    });
  });

  await page.goto("/home");
  await waitForAppReady(page);
  await openAccountDrawer(page);

  await expect(
    page.getByText("Unable to load account details. Please try again."),
  ).toBeVisible();
});
