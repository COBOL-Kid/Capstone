import { APIRequestContext } from "@playwright/test";
import { bootstrapCsrf, loginViaApi, readXsrfToken } from "./auth.fixture";
import { SEEDED_VINS, TEST_USER } from "./test-data";

async function mutatingHeaders(
  request: APIRequestContext,
): Promise<Record<string, string>> {
  return {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": readXsrfToken(await request.storageState()),
  };
}

export async function addVehicleViaApi(
  request: APIRequestContext,
  vin: string,
  currentMileage: number,
): Promise<void> {
  const headers = await mutatingHeaders(request);
  const response = await request.post("/api/vin", {
    headers,
    data: { vin, currentMileage },
  });
  if (!response.ok()) {
    throw new Error(
      `Add vehicle failed: ${response.status()} ${await response.text()}`,
    );
  }
}

export async function deleteVehicleViaApi(
  request: APIRequestContext,
  vin: string,
): Promise<void> {
  const headers = await mutatingHeaders(request);
  const response = await request.delete(`/api/vin/${vin}`, { headers });
  if (!response.ok() && response.status() !== 404) {
    throw new Error(
      `Delete vehicle failed: ${response.status()} ${await response.text()}`,
    );
  }
}

export async function updateMileageViaApi(
  request: APIRequestContext,
  vin: string,
  currentMileage: number,
): Promise<void> {
  const headers = await mutatingHeaders(request);
  const response = await request.patch(`/api/vin/${vin}/mileage`, {
    headers,
    data: { currentMileage },
  });
  if (!response.ok()) {
    throw new Error(
      `Update mileage failed: ${response.status()} ${await response.text()}`,
    );
  }
}

export async function ensureCamryLinkedToTestUser(
  request: APIRequestContext,
): Promise<void> {
  await loginViaApi(request, TEST_USER);
  const listResponse = await request.get("/api/vin");
  if (listResponse.ok()) {
    const vehicles = (await listResponse.json()) as Array<{ vin: string }>;
    if (vehicles.some((vehicle) => vehicle.vin === SEEDED_VINS.camry)) {
      return;
    }
  }
  await addVehicleViaApi(request, SEEDED_VINS.camry, 45_200);
}

export async function resetCamryMileage(
  request: APIRequestContext,
): Promise<void> {
  await loginViaApi(request, TEST_USER);
  await updateMileageViaApi(request, SEEDED_VINS.camry, 45_200);
}

export async function withAuthenticatedRequest(
  credentials = TEST_USER,
): Promise<APIRequestContext> {
  const { request } = await import("@playwright/test");
  const context = await request.newContext({
    baseURL: "http://localhost:4200",
  });
  await bootstrapCsrf(context);
  await loginViaApi(context, credentials);
  return context;
}
