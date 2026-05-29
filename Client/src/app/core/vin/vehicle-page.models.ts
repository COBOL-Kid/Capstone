import {
  CompletedMaintenanceResponse,
  MiscMaintenanceCostResponse,
  UpcomingMaintenanceIntervalResponse,
} from '../maintenance/maintenance.models';
import { CompletedRecallResponse, RecallResponse } from '../recall/recall.models';
import { VehicleDetailResponse } from './vin.models';

export interface VehiclePageData {
  detail: VehicleDetailResponse;
  upcomingIntervals: UpcomingMaintenanceIntervalResponse[];
  completedMaintenance: CompletedMaintenanceResponse[];
  uncompletedRecalls: RecallResponse[];
  completedRecalls: CompletedRecallResponse[];
  miscMaintenanceCosts: MiscMaintenanceCostResponse[];
}
