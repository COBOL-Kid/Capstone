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
  selectedTrim?: string;
}

export interface AddVinTrimSelectionRequiredResponse {
  requiresTrimSelection: true;
  year: string;
  make: string;
  model: string;
}

export interface TrimOptionsResponse {
  trims: string[];
}

export type AddVehicleResult =
  | { kind: 'completed'; response: AddVinResponse }
  | { kind: 'trimSelectionRequired'; context: AddVinTrimSelectionRequiredResponse };

export function isAddVinTrimSelectionRequiredResponse(
  value: AddVinResponse | AddVinTrimSelectionRequiredResponse,
): value is AddVinTrimSelectionRequiredResponse {
  return 'requiresTrimSelection' in value && value.requiresTrimSelection === true;
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
import { VehicleWarrantyResponse } from '../warranty/warranty.models';

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

export interface UpdateVehiclePhotoRequest {
  selectedImageUrl: string;
}

export interface VehicleDashboardResponse {
  detail: VehicleDetailResponse;
  upcomingMaintenance: UpcomingMaintenanceIntervalResponse[];
  completedMaintenance: CompletedMaintenanceResponse[];
  uncompletedRecalls: RecallResponse[];
  completedRecalls: CompletedRecallResponse[];
  miscMaintenanceCosts: MiscMaintenanceCostResponse[];
  vehicleWarranty: VehicleWarrantyResponse | null;
}
