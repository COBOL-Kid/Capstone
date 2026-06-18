import { expect, test } from "@playwright/test";
import { waitForAppReady } from "../../fixtures/ui.helpers";

test.describe("Public pages and navigation", () => {
  test("PUB-001: landing page shows brand and auth entry points", async ({
    page,
  }) => {
    await page.goto("/");
    await waitForAppReady(page);

    await expect(
      page.getByRole("heading", { name: "Honest Car" }),
    ).toBeVisible();
    await expect(
      page.getByText("Everything you need to know about your car"),
    ).toBeVisible();
    await expect(page.getByRole("link", { name: "Sign Up" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Sign In" })).toBeVisible();
  });

  test("PUB-002: sign-in deep link opens the sign-in modal", async ({
    page,
  }) => {
    await page.goto("/sign-in");
    await waitForAppReady(page);

    await expect(page).toHaveURL(/\/sign-in$/);
    await expect(page.getByRole("dialog")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Welcome back" }),
    ).toBeVisible();
    await expect(page.getByLabel("Email")).toBeVisible();
    await expect(page.getByLabel("Password")).toBeVisible();
  });

  test("PUB-003: sign-up deep link opens the sign-up modal", async ({
    page,
  }) => {
    await page.goto("/sign-up");
    await waitForAppReady(page);

    await expect(page).toHaveURL(/\/sign-up$/);
    await expect(page.getByRole("dialog")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Create your account" }),
    ).toBeVisible();
    await expect(page.getByLabel("First name")).toBeVisible();
    await expect(page.getByLabel("Last name")).toBeVisible();
    await expect(page.getByLabel("Email")).toBeVisible();
    await expect(page.getByLabel("Password")).toBeVisible();
  });

  test("PUB-004: auth modal mode switching works", async ({ page }) => {
    await page.goto("/sign-in");
    await waitForAppReady(page);

    await page
      .getByRole("dialog")
      .getByRole("link", { name: "Sign up" })
      .click();
    await expect(page).toHaveURL(/\/sign-up$/);
    await expect(
      page.getByRole("heading", { name: "Create your account" }),
    ).toBeVisible();

    await page
      .getByRole("dialog")
      .getByRole("link", { name: "Sign in" })
      .click();
    await expect(page).toHaveURL(/\/sign-in$/);
    await expect(
      page.getByRole("heading", { name: "Welcome back" }),
    ).toBeVisible();
  });

  test("PUB-005: closing auth modal from deep link returns to landing", async ({
    page,
  }) => {
    await page.goto("/sign-in");
    await waitForAppReady(page);

    await page
      .getByRole("button", { name: "Close authentication dialog" })
      .click();
    await expect(page.getByRole("dialog")).toBeHidden();
    await expect(page).toHaveURL("/");
  });

  test("PUB-006: our services page renders public content", async ({
    page,
  }) => {
    await page.goto("/our-services");
    await waitForAppReady(page);

    await expect(
      page.getByRole("heading", { name: "Honest Car" }),
    ).toBeVisible();
    await expect(page.getByText("Maintenance, planned")).toBeVisible();
    await expect(page.getByText("Recalls, caught early")).toBeVisible();
    await expect(page.getByText("Warranty, clarified")).toBeVisible();
    await expect(
      page.getByRole("link", { name: "support@honest-car.co" }),
    ).toBeVisible();
  });

  test("PUB-007: navbar public navigation works", async ({ page }) => {
    await page.goto("/our-services");
    await waitForAppReady(page);

    await page.getByRole("link", { name: "Honest Car" }).click();
    await expect(page).toHaveURL("/");

    await page.getByRole("link", { name: "Our Services" }).click();
    await expect(page).toHaveURL("/our-services");
  });

  test("PUB-008: unknown route redirects safely", async ({ page }) => {
    await page.goto("/does-not-exist");
    await waitForAppReady(page);

    await expect(page).toHaveURL("/");
    await expect(page.getByRole("link", { name: "Sign In" })).toBeVisible();
  });
});
