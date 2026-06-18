import { expect, Locator, Page } from "@playwright/test";
import { TEST_USER } from "./test-data";

export async function waitForAppReady(page: Page): Promise<void> {
  await page
    .locator(".app-splash")
    .waitFor({ state: "detached" })
    .catch(() => {
      /* splash may already be gone */
    });
  await expect(page.getByRole("navigation", { name: "Primary" })).toBeVisible();
}

export async function waitForGarageReady(page: Page): Promise<void> {
  await waitForAppReady(page);
  await expect(page).toHaveURL(/\/home$/);
  await expect(
    page.getByRole("heading", { name: "My Vehicles" }),
  ).toBeVisible();
}

export async function signInViaUI(
  page: Page,
  credentials = TEST_USER,
): Promise<void> {
  await page.goto("/sign-in");
  await waitForAppReady(page);
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByLabel("Email").fill(credentials.email);
  await page.getByLabel("Password").fill(credentials.password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/\/home$/);
}

export async function openAccountDrawer(page: Page): Promise<void> {
  await page.getByRole("button", { name: "Account" }).click();
  await expect(page.getByRole("dialog", { name: "Account" })).toBeVisible();
}

export async function closeAccountDrawer(page: Page): Promise<void> {
  await page.getByRole("button", { name: "Close account panel" }).click();
  await expect(page.getByRole("dialog", { name: "Account" })).toBeHidden();
}

export async function signOutViaUI(page: Page): Promise<void> {
  await openAccountDrawer(page);
  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page).toHaveURL("/");
}

export async function expectToast(
  page: Page,
  message: string,
  variant: "success" | "error" | "info" = "success",
): Promise<void> {
  const toast = page.locator(`.hc-toast--${variant}`, { hasText: message });
  await expect(toast).toBeVisible();
}

export async function openAddVehicleModal(page: Page): Promise<void> {
  const headerButton = page.getByRole("button", { name: "Add new vehicle" });
  const emptyButton = page.getByRole("button", { name: "Add a vehicle" });
  if (await headerButton.isVisible()) {
    await headerButton.click();
  } else {
    await emptyButton.click();
  }
  await expect(
    page.getByRole("dialog", { name: "Add a vehicle" }),
  ).toBeVisible();
}

export async function navigateToVehicleDetail(
  page: Page,
  label: string,
): Promise<void> {
  await page.getByRole("link", { name: label }).click();
  await expect(
    page.getByRole("heading", { name: label, level: 1 }),
  ).toBeVisible();
}

export function addVehicleDialog(page: Page): Locator {
  return page.getByRole("dialog", { name: "Add a vehicle" });
}

export async function submitAddVehicleForm(page: Page): Promise<void> {
  await addVehicleDialog(page)
    .getByRole("button", { name: "Add Vehicle", exact: true })
    .click();
}

export function vehicleDetailToggle(page: Page): Locator {
  return page.locator(".vehicle-detail__toggle");
}

export async function waitForVehicleDetailSettled(page: Page): Promise<void> {
  await expect(
    page
      .locator(".vehicle-detail__title, .vehicle-detail__status--error")
      .first(),
  ).toBeVisible({ timeout: 20_000 });
}

export function selectVehiclePhoto(page: Page, index: number): Locator {
  return page.getByRole("listitem", { name: `Select photo ${index}` });
}

export function maintenanceDetailDialog(page: Page): Locator {
  return page.getByRole("dialog", { name: "Maintenance" });
}

export function recallDetailDialog(page: Page): Locator {
  return page.getByRole("dialog", { name: "Recall" });
}

/** Opens the complete form inside an already-visible maintenance detail dialog. */
export async function openMaintenanceCompleteForm(
  page: Page,
): Promise<Locator> {
  const dialog = maintenanceDetailDialog(page);
  await dialog.getByRole("button", { name: "Mark complete" }).click();
  return dialog;
}

/** Opens the complete form inside an already-visible recall detail dialog. */
export async function openRecallCompleteForm(page: Page): Promise<Locator> {
  const dialog = recallDetailDialog(page);
  await dialog.getByRole("button", { name: "Mark complete" }).click();
  return dialog;
}

export async function closeDialogViaBackdrop(
  page: Page,
  dialogName: string | RegExp,
): Promise<void> {
  const dialog = page.getByRole("dialog", { name: dialogName });
  await page
    .locator(".hc-overlay-backdrop")
    .click({ position: { x: 5, y: 5 } });
  await expect(dialog).toBeHidden();
}

export async function submitVerificationCode(
  page: Page,
  code: string,
  submitLabel = "Verify email",
): Promise<void> {
  await page.getByLabel("Verification code").fill(code);
  await page.getByRole("button", { name: submitLabel, exact: true }).click();
}

export async function assertNoAuthTokensInWebStorage(
  page: Page,
): Promise<void> {
  const storage = await page.evaluate(() => {
    const keys = [...Object.keys(localStorage), ...Object.keys(sessionStorage)];
    const suspicious = keys.filter(
      (key) =>
        /token|jwt|access|refresh|session/i.test(key) &&
        !key.toLowerCase().includes("xsrf"),
    );
    return { suspicious, localStorage, sessionStorage };
  });
  expect(storage.suspicious).toEqual([]);
}

export function mockSlowRoute(
  page: Page,
  urlPattern: string | RegExp,
  delayMs: number,
  method?: string,
): Promise<void> {
  return page.route(urlPattern, async (route) => {
    if (method && route.request().method() !== method) {
      await route.continue();
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, delayMs));
    await route.continue();
  });
}
