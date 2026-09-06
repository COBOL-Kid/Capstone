import type { VehicleDashboardResponse } from '$lib/models/vin';
import type { VehiclePageData } from '$lib/models/vehicle-page';
import { getVehicleDashboard } from './vin';

export async function loadVehiclePage(vin: string): Promise<VehiclePageData> {
  const dashboard = await getVehicleDashboard(vin);
  return toVehiclePageData(dashboard);
}

export function toVehiclePageData(dashboard: VehicleDashboardResponse): VehiclePageData {
  return {
    detail: dashboard.detail,
    upcomingIntervals: dashboard.upcomingMaintenance,
    completedMaintenance: dashboard.completedMaintenance,
    uncompletedRecalls: dashboard.uncompletedRecalls,
    completedRecalls: dashboard.completedRecalls,
    miscMaintenanceCosts: dashboard.miscMaintenanceCosts,
    vehicleWarranty: dashboard.vehicleWarranty,
  };
}
