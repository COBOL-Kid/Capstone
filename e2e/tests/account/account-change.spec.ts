import {
  passwordChangeTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { VERIFICATION_CODE } from "../../fixtures/test-data";
import {
  closeAccountDrawer,
  expectToast,
  openAccountDrawer,
  submitVerificationCode,
  waitForAppReady,
} from "../../fixtures/ui.helpers";

test.describe.configure({ mode: "serial" });

test("ACCT-004: change SMS happy path", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change SMS" }).click();
  await page.getByLabel("New SMS phone number").fill("+15551112222");
  await page.getByRole("button", { name: "Send verification code" }).click();
  await submitVerificationCode(page, VERIFICATION_CODE, "Confirm SMS change");

  await expectToast(page, "SMS number updated.");
  await expect(page.getByText("+15551112222")).toBeVisible();
});

test("ACCT-005: remove SMS number", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change SMS" }).click();
  await page.getByLabel("New SMS phone number").fill("");
  await page.getByRole("button", { name: "Send verification code" }).click();
  await submitVerificationCode(page, VERIFICATION_CODE, "Confirm SMS change");

  await expectToast(page, "SMS number updated.");
  await expect(page.getByText("Not provided")).toBeVisible();
});

test("ACCT-011: pending account change resumes verification step", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change SMS" }).click();
  await page.getByLabel("New SMS phone number").fill("+15553334444");
  await page.getByRole("button", { name: "Send verification code" }).click();
  await expect(page.getByLabel("Verification code")).toBeVisible();

  await page.keyboard.press("Escape");
  await expect(
    page.getByRole("dialog", { name: "Change SMS Number" }),
  ).toBeHidden();
  await closeAccountDrawer(page);
  await openAccountDrawer(page);

  await expect(page.getByLabel("Verification code")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Confirm SMS change" }),
  ).toBeVisible();
});

test("ACCT-012: resend account-change code disables controls while pending", async ({
  page,
}) => {
  await page.route("**/api/account/change-requests/resend", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2_000));
    await route.continue();
  });

  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change SMS" }).click();
  await page.getByLabel("New SMS phone number").fill("+15554445555");
  await page.getByRole("button", { name: "Send verification code" }).click();

  const resendButton = page.getByRole("button", { name: "Resend code" });
  await resendButton.click();
  await expect(page.getByRole("button", { name: "Sending…" })).toBeVisible({
    timeout: 5_000,
  });
});

test("ACCT-007: change email happy path", async ({ page }) => {
  const newEmail = `e2e-email-${Date.now()}@example.com`;

  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change Email" }).click();
  await page.getByLabel("New email address").fill(newEmail);
  await page.getByRole("button", { name: "Send verification code" }).click();
  await submitVerificationCode(page, VERIFICATION_CODE, "Confirm email change");

  await expectToast(page, "Email updated.");
  await expect(page.getByText(newEmail)).toBeVisible();
});
