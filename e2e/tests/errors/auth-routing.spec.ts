import {
  authenticatedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test("ADD-002: signed-in user can close sign-in modal and return to garage", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await page.goto("/sign-in");
  await waitForAppReady(page);
  await expect(page.getByRole("dialog")).toBeVisible();
  await page
    .getByRole("button", { name: "Close authentication dialog" })
    .click();
  await expect(page).toHaveURL(/\/home$/);
});

test("ADD-002b: signed-in user can close sign-up modal and return to garage", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await page.goto("/sign-up");
  await waitForAppReady(page);
  await expect(page.getByRole("dialog")).toBeVisible();
  await page
    .getByRole("button", { name: "Close authentication dialog" })
    .click();
  await expect(page).toHaveURL(/\/home$/);
});
