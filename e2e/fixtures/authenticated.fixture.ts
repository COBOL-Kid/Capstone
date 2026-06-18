import { test as base, expect } from "@playwright/test";
import {
  loginUnverifiedViaRefreshToken,
  loginViaApi,
  resetUnverifiedRefreshToken,
} from "./auth.fixture";
import { EMPTY_GARAGE_USER, TEST_USER } from "./test-data";

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
    resetUnverifiedRefreshToken();
    const context = await browser.newContext();
    await loginUnverifiedViaRefreshToken(context.request);
    await use(context);
    await context.close();
  },
});

export { expect };
