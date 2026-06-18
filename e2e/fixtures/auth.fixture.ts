import { execSync } from "node:child_process";
import { APIRequestContext, test as base } from "@playwright/test";
import {
  EMPTY_GARAGE_USER,
  TEST_USER,
  UNVERIFIED_REFRESH_TOKEN,
  UNVERIFIED_USER,
} from "./test-data";

type Credentials = { email: string; password: string };

export function readXsrfToken(storageState: {
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

export async function bootstrapCsrf(
  request: APIRequestContext,
): Promise<string> {
  const csrfResponse = await request.get("/api/auth/csrf");
  if (!csrfResponse.ok()) {
    throw new Error(`CSRF bootstrap failed: ${csrfResponse.status()}`);
  }
  return readXsrfToken(await request.storageState());
}

export async function loginViaApi(
  request: APIRequestContext,
  credentials: Credentials = TEST_USER,
): Promise<void> {
  const xsrfToken = await bootstrapCsrf(request);

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

export async function loginUnverifiedViaRefreshToken(
  request: APIRequestContext,
): Promise<void> {
  const xsrfToken = await bootstrapCsrf(request);

  const refreshResponse = await request.post("/api/auth/refresh", {
    headers: {
      "X-XSRF-TOKEN": xsrfToken,
      Cookie: `refreshToken=${UNVERIFIED_REFRESH_TOKEN}; XSRF-TOKEN=${xsrfToken}`,
    },
  });

  if (!refreshResponse.ok()) {
    throw new Error(
      `Unverified refresh login failed: ${refreshResponse.status()} ${await refreshResponse.text()}`,
    );
  }

  const sessionCookie = (await request.storageState()).cookies.find(
    (cookie) => cookie.name === "__session",
  );
  if (!sessionCookie) {
    throw new Error("Missing __session cookie after refresh login");
  }
}

export async function logoutViaApi(request: APIRequestContext): Promise<void> {
  const xsrfToken = readXsrfToken(await request.storageState());
  await request.post("/api/auth/logout", {
    headers: { "X-XSRF-TOKEN": xsrfToken },
  });
}

export function resetUnverifiedRefreshToken(): void {
  execSync(
    `PGPASSWORD=honestcar psql -h localhost -U honestcar -d honestcar -v ON_ERROR_STOP=1 -c "DELETE FROM refresh_token WHERE user_id = 3; INSERT INTO refresh_token (token, expiry_date, user_id) VALUES ('${UNVERIFIED_REFRESH_TOKEN}', CURRENT_TIMESTAMP + INTERVAL '30 days', 3);"`,
    { stdio: "pipe" },
  );
}

export { EMPTY_GARAGE_USER, TEST_USER, UNVERIFIED_USER };
