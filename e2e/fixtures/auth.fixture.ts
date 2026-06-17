import { APIRequestContext, test as base } from "@playwright/test";
import { TEST_USER } from "./test-data";

type Credentials = typeof TEST_USER;

export async function loginViaApi(
  request: APIRequestContext,
  credentials: Credentials = TEST_USER,
): Promise<void> {
  const csrfResponse = await request.get("/api/auth/csrf");
  if (!csrfResponse.ok()) {
    throw new Error(`CSRF bootstrap failed: ${csrfResponse.status()}`);
  }

  const xsrfToken = readXsrfToken(await request.storageState());

  const loginResponse = await request.post("/api/auth/authenticate", {
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": xsrfToken,
    },
    data: {
      email: credentials.email,
      password: credentials.password,
    },
  });

  if (!loginResponse.ok()) {
    throw new Error(
      `Login failed: ${loginResponse.status()} ${await loginResponse.text()}`,
    );
  }

  const sessionCookie = (await request.storageState()).cookies.find(
    (cookie) => cookie.name === "__session",
  );
  if (!sessionCookie) {
    throw new Error("Missing __session cookie after login");
  }
}

function readXsrfToken(storageState: {
  cookies: Array<{ name: string; value: string }>;
}): string {
  const token = storageState.cookies.find(
    (cookie) => cookie.name === "XSRF-TOKEN",
  );
  if (!token?.value) {
    throw new Error("Missing XSRF-TOKEN cookie after CSRF bootstrap");
  }
  return token.value;
}

export const authenticatedTest = base.extend({
  storageState: ".auth/user.json",
});
