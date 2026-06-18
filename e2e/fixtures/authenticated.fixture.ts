import { test as base, expect } from "@playwright/test";
import {
  loginUnverifiedViaRefreshToken,
  loginViaApi,
  resetUnverifiedRefreshToken,
} from "./auth.fixture";
import { resetPasswordChangeUser, resetUnverifiedUser } from "./db.helpers";
import {
  EMPTY_GARAGE_USER,
  PASSWORD_CHANGE_USER,
  TEST_USER,
} from "./test-data";

export const authenticatedTest = base.extend({
  context: async ({ browser }, use) => {
    const context = await browser.newContext();
    await loginViaApi(context.request, TEST_USER);
    await use(context);
    await context.close();
  },
});

export const emptyGarageTest = base.extend({
  context: async ({ browser }, use) => {
    const context = await browser.newContext();
    await loginViaApi(context.request, EMPTY_GARAGE_USER);
    await use(context);
    await context.close();
  },
});

export const unverifiedTest = base.extend({
  context: async ({ browser }, use) => {
    resetUnverifiedUser();
    const context = await browser.newContext();
    await loginUnverifiedViaRefreshToken(context.request);
    await use(context);
    await context.close();
  },
});

export const passwordChangeTest = base.extend({
  context: async ({ browser }, use) => {
    resetPasswordChangeUser();
    const context = await browser.newContext();
    await loginViaApi(context.request, PASSWORD_CHANGE_USER);
    await use(context);
    await context.close();
  },
});

export { expect };
