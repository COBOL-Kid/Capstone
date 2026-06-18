import {
  passwordChangeTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import {
  PASSWORD_CHANGE_USER,
  VERIFICATION_CODE,
} from "../../fixtures/test-data";
import {
  expectToast,
  openAccountDrawer,
  submitVerificationCode,
  waitForAppReady,
} from "../../fixtures/ui.helpers";

test.describe.configure({ mode: "serial" });

test("ACCT-009: change password revokes session after verification", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Change Password" }).click();

  const passwordDialog = page.getByRole("dialog", { name: "Change Password" });
  await passwordDialog
    .getByLabel("Current password")
    .fill(PASSWORD_CHANGE_USER.password);
  await passwordDialog
    .getByRole("textbox", { name: "New password", exact: true })
    .fill(PASSWORD_CHANGE_USER.newPassword);
  await passwordDialog
    .getByLabel("Confirm new password")
    .fill(PASSWORD_CHANGE_USER.newPassword);
  await passwordDialog
    .getByRole("button", { name: "Send verification code" })
    .click();

  await submitVerificationCode(
    page,
    VERIFICATION_CODE,
    "Confirm password change",
  );

  await expectToast(
    page,
    "Password updated. Please sign in with your new password.",
  );
  await expect(page).toHaveURL(/\/sign-in$/);

  await page.goto("/home");
  await expect(page).toHaveURL(/\/sign-in$/);

  await page.getByLabel("Email").fill(PASSWORD_CHANGE_USER.email);
  await page.getByLabel("Password").fill(PASSWORD_CHANGE_USER.password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();

  await page.getByLabel("Password").fill(PASSWORD_CHANGE_USER.newPassword);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/home$/);
});
