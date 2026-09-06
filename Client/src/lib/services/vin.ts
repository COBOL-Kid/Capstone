import { ApiError, apiDelete, apiGet, apiPatch, apiPost } from '$lib/api/client';
import { apiConfig } from '$lib/api/config';
import { normalizeListResponse, toFieldErrorMessage } from '$lib/api/errors';
import type { VehicleListingsResponse } from '$lib/models/listings';
import type {
  AddVehicleResult,
  AddVinRequest,
  AddVinResponse,
  AddVinTrimSelectionRequiredResponse,
  TrimOptionsResponse,
  UpdateMileageRequest,
  UpdateVehiclePhotoRequest,
  UserVehicleResponse,
  VehicleDashboardResponse,
  VehicleDetailResponse,
  VinErrorMessage,
} from '$lib/models/vin';
import { isAddVinTrimSelectionRequiredResponse } from '$lib/models/vin';

const vinDataUnavailableMessage =
  "We don't have vehicle information for this VIN in our system yet. Try a different VIN or check back later as we add more vehicles.";
const vinNotFoundBackendMessages = new Set([
  'Vehicle not found for VIN',
  'Vehicle data is not available for this VIN',
]);
const onboardingUnavailableMessage =
  "We couldn't load vehicle data for this vehicle right now. Please try again later.";
const genericErrorMessage = 'Unable to add the vehicle. Please try again.';
const emailNotVerifiedMessage =
  'Verify your email before adding vehicles. Use the banner on this page to enter your code.';

function toVinErrorMessage(error: ApiError): VinErrorMessage {
  if (error.status === 403 && typeof error.body === 'string') {
    const message = error.body.trim();
    if (message.includes('Email address must be verified')) {
      return { message: emailNotVerifiedMessage, fieldMessages: [] };
    }
  }

  if (isVinDataUnavailableError(error)) {
    return { message: vinDataUnavailableMessage, fieldMessages: [] };
  }

  if (error.status >= 500) {
    return { message: onboardingUnavailableMessage, fieldMessages: [] };
  }

  const fieldError = toFieldErrorMessage(error.body, genericErrorMessage);
  if (fieldError.message !== genericErrorMessage || fieldError.fieldMessages.length > 0) {
    return fieldError;
  }

  return { message: genericErrorMessage, fieldMessages: [] };
}

function isVinDataUnavailableError(error: ApiError): boolean {
  if (error.status === 404) {
    return true;
  }
  if (typeof error.body !== 'string') {
    return false;
  }
  return vinNotFoundBackendMessages.has(error.body.trim());
}

export async function getUserVehicles(): Promise<UserVehicleResponse[]> {
  const vehicles = await apiGet<UserVehicleResponse[] | null>(apiConfig.vinUrl);
  return normalizeListResponse(vehicles);
}

export function getVehicleDetail(vin: string): Promise<VehicleDetailResponse> {
  return apiGet<VehicleDetailResponse>(`${apiConfig.vinUrl}/${vin}`);
}

export function getVehicleDashboard(vin: string): Promise<VehicleDashboardResponse> {
  return apiGet<VehicleDashboardResponse>(`${apiConfig.vinUrl}/${vin}/dashboard`);
}

export function getVehicleListings(vin: string): Promise<VehicleListingsResponse> {
  return apiGet<VehicleListingsResponse>(`${apiConfig.vinUrl}/${vin}/listings`);
}

export function updateMileage(
  vin: string,
  request: UpdateMileageRequest,
): Promise<VehicleDetailResponse> {
  return apiPatch<VehicleDetailResponse>(`${apiConfig.vinUrl}/${vin}/mileage`, request);
}

export function updateSelectedPhoto(
  vin: string,
  request: UpdateVehiclePhotoRequest,
): Promise<UserVehicleResponse> {
  return apiPatch<UserVehicleResponse>(`${apiConfig.vinUrl}/${vin}/photo`, request);
}

export async function getTrimOptions(year: string, make: string, model: string): Promise<string[]> {
  const params = new URLSearchParams({ year, make, model });
  const response = await apiGet<TrimOptionsResponse>(
    `${apiConfig.vinUrl}/trim-options?${params.toString()}`,
  );
  return response.trims ?? [];
}

export async function addVehicle(request: AddVinRequest): Promise<AddVehicleResult> {
  let body: AddVinResponse | AddVinTrimSelectionRequiredResponse;
  try {
    body = await apiPost<AddVinResponse | AddVinTrimSelectionRequiredResponse>(
      apiConfig.vinUrl,
      request,
    );
  } catch (error) {
    if (error instanceof ApiError) {
      throw toVinErrorMessage(error);
    }
    throw { message: genericErrorMessage, fieldMessages: [] } satisfies VinErrorMessage;
  }
  if (isAddVinTrimSelectionRequiredResponse(body)) {
    return { kind: 'trimSelectionRequired', context: body };
  }
  return { kind: 'completed', response: body };
}

export function deleteVehicle(vin: string): Promise<void> {
  return apiDelete<void>(`${apiConfig.vinUrl}/${vin}`);
}
