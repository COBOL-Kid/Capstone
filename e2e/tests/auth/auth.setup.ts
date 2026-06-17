import { test as setup } from "@playwright/test";
import { loginViaApi } from "../../fixtures/auth.fixture";

const authFile = ".auth/user.json";

setup("authenticate test user", async ({ request }) => {
  await loginViaApi(request);
  await request.storageState({ path: authFile });
});
