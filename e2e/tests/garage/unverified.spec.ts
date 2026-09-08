import {
  unverifiedTest as test,
  expect,
} from "../../fixtures/authenticated.fixture";
import { VERIFICATION_CODE } from "../../fixtures/test-data";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test.describe.configure({ mode: "serial" });

test("GAR-010: unverified user does not fetch or show vehicles", async ({
  page,
}) => {
  const vinRequests: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/api/vin") && request.method() === "GET") {
      vinRequests.push(request.url());
    }
  });

  await page.goto("/home");
  await waitForAppReady(page);

  await expect(page.getByText("Verify your email")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add a vehicle" }),
  ).toBeDisabled();
  await expect(
    page.getByRole("heading", { name: "2020 Toyota Camry" }),
  ).toHaveCount(0);
  expect(vinRequests).toHaveLength(0);
});

test("VIN-011: unverified user cannot add vehicle", async ({ page }) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await expect(
    page.getByRole("button", { name: "Add a vehicle" }),
  ).toBeDisabled();
});

test("AUTH-013: email verification invalid code shows error", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await page.getByRole("button", { name: "Enter code" }).click();
  await page.getByLabel("Verification code").fill("000000");
  await page.getByRole("button", { name: "Verify email" }).click();

  await expect(page.getByRole("alert")).toContainText(
    /Invalid verification code/i,
  );
  await expect(
    page.getByRole("button", { name: "Add a vehicle" }),
  ).toBeDisabled();
});

test("AUTH-012: email verification from home banner succeeds", async ({
  page,
}) => {
  await page.goto("/home");
  await waitForAppReady(page);

  await page.getByRole("button", { name: "Enter code" }).click();
  await page.getByLabel("Verification code").fill(VERIFICATION_CODE);
  await page.getByRole("button", { name: "Verify email" }).click();

  await expect(page.getByText("Verify your email")).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Add a vehicle" }),
  ).toBeEnabled();
});
