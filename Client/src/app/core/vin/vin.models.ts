export interface UserVehicleResponse {
  vin: string;
  currentMileage: number;
  vehicleTypeId: number;
  make: string;
  model: string;
  trim: string;
  year: string;
  availableImageUrls: string[];
  selectedImageUrl: string;
}

export interface AddVinRequest {
  vin: string;
  currentMileage: number;
}

export interface AddVinResponse {
  vin: string;
  currentMileage: number;
  vehicleTypeId: number;
  make: string;
  model: string;
  trim: string;
  year: string;
  availableImageUrls: string[];
  selectedImageUrl: string;
  createdVin: boolean;
  createdVehicleType: boolean;
  createdAssociation: boolean;
}

import { FieldErrorMessage } from '../http/http-error.util';
import {
  CompletedMaintenanceResponse,
  MiscMaintenanceCostResponse,
  UpcomingMaintenanceIntervalResponse,
} from '../maintenance/maintenance.models';
import { CompletedRecallResponse, RecallResponse } from '../recall/recall.models';

export type VinErrorMessage = FieldErrorMessage;

export interface VehicleDetailResponse {
  vin: string;
  vehicleTypeId: number;
  vehicleMake: string;
  vehicleModel: string;
  vehicleTrim: string;
  vehicleYear: string;
  vehicleStyle: string;
  sourceVin: string | null;
  origin: string | null;
  body: string | null;
  engineDescription: string | null;
  transmissionStyle: string | null;
  driveType: string | null;
  ownersManual: string | null;
  currentMileage: number;
  availableImageUrls: string[];
  selectedImageUrl: string;
}

export interface UpdateMileageRequest {
  currentMileage: number;
}

export interface VehicleDashboardResponse {
  detail: VehicleDetailResponse;
  upcomingMaintenance: UpcomingMaintenanceIntervalResponse[];
  completedMaintenance: CompletedMaintenanceResponse[];
  uncompletedRecalls: RecallResponse[];
  completedRecalls: CompletedRecallResponse[];
  miscMaintenanceCosts: MiscMaintenanceCostResponse[];
}
