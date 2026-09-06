import type {
  CompletedMaintenanceResponse,
  MiscMaintenanceCostResponse,
  UpcomingMaintenanceIntervalResponse,
} from './maintenance';
import type { CompletedRecallResponse, RecallResponse } from './recall';
import type { VehicleWarrantyResponse } from './warranty';
import type { VehicleDetailResponse } from './vin';

export interface VehiclePageData {
  detail: VehicleDetailResponse;
  upcomingIntervals: UpcomingMaintenanceIntervalResponse[];
  completedMaintenance: CompletedMaintenanceResponse[];
  uncompletedRecalls: RecallResponse[];
  completedRecalls: CompletedRecallResponse[];
  miscMaintenanceCosts: MiscMaintenanceCostResponse[];
  vehicleWarranty: VehicleWarrantyResponse | null;
}
