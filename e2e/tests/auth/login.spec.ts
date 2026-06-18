import { expect, test } from "@playwright/test";
import { TEST_USER } from "../../fixtures/test-data";
import {
  openAccountDrawer,
  signInViaUI,
  waitForAppReady,
} from "../../fixtures/ui.helpers";

test.describe("Authentication", () => {
  test("AUTH-001: successful sign-in via UI reaches garage", async ({
    page,
  }) => {
    await signInViaUI(page);

    await expect(
      page.getByRole("heading", { name: "My Vehicles" }),
    ).toBeVisible();
    await openAccountDrawer(page);
    await expect(page.getByText(TEST_USER.email)).toBeVisible();
    await expect(
      page.getByText(`${TEST_USER.firstName} ${TEST_USER.lastName}`),
    ).toBeVisible();
  });

  test(
    "AUTH-002: invalid credentials show error without authenticating",
    { retries: 0 },
    async ({ page }) => {
      await page.goto("/sign-in");
      await waitForAppReady(page);

      await page.getByLabel("Email").fill(TEST_USER.email);
      await page.getByLabel("Password").fill("WrongPassword123!");
      await page.getByRole("button", { name: "Sign In", exact: true }).click();

      await expect(page.getByRole("alert")).toContainText(
        /Invalid account credentials|Invalid email or password/i,
      );
      await expect(page.getByRole("dialog")).toBeVisible();
      await expect(
        page.getByRole("heading", { name: "My Vehicles" }),
      ).toHaveCount(0);
    },
  );

  test("AUTH-008: registration form validation blocks weak sign-up", async ({
    page,
  }) => {
    await page.goto("/sign-up");
    await waitForAppReady(page);

    await page.getByRole("button", { name: "Sign Up", exact: true }).click();

    await expect(page.getByText("First name is required.")).toBeVisible();
    await expect(page.getByText("Last name is required.")).toBeVisible();
    await expect(page.getByText("Email is required.")).toBeVisible();
    await expect(page.getByText("Password is required.")).toBeVisible();

    await page.getByLabel("Email").fill("not-an-email");
    await page.getByLabel("Password").fill("short");
    await page.getByRole("button", { name: "Sign Up", exact: true }).click();

    await expect(page.getByText("Enter a valid email address.")).toBeVisible();
    await expect(
      page.getByText("Password must be at least 8 characters."),
    ).toBeVisible();
  });

  test("AUTH-009: duplicate registration email returns friendly error", async ({
    page,
  }) => {
    await page.goto("/sign-up");
    await waitForAppReady(page);

    await page.getByLabel("First name").fill("Another");
    await page.getByLabel("Last name").fill("User");
    await page.getByLabel("Email").fill(TEST_USER.email);
    await page.getByLabel("Password").fill("Password123!");
    await page.getByRole("button", { name: "Sign Up", exact: true }).click();

    await expect(page.getByRole("alert")).toContainText(
      /account already exists/i,
    );
  });
});

test.describe("Auth guard", () => {
  test("AUTH-003: unauthenticated home visit redirects to sign-in", async ({
    page,
  }) => {
    await page.goto("/home");

    await expect(page).toHaveURL(/\/sign-in$/);
    await expect(page.getByRole("dialog")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Welcome back" }),
    ).toBeVisible();
  });
});
