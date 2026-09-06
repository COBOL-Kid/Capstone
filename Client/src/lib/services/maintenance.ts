import { apiDelete, apiPost } from '$lib/api/client';
import { apiConfig } from '$lib/api/config';
import type {
  CompleteMaintenanceRequest,
  CompletedMaintenanceResponse,
} from '$lib/models/maintenance';

export function completeMaintenance(
  request: CompleteMaintenanceRequest,
): Promise<CompletedMaintenanceResponse> {
  return apiPost<CompletedMaintenanceResponse>(`${apiConfig.maintenanceUrl}/completed`, request);
}

export function uncompleteMaintenance(completedMaintenanceId: number): Promise<void> {
  return apiDelete<void>(`${apiConfig.maintenanceUrl}/completed/${completedMaintenanceId}`);
}
