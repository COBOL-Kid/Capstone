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

export interface VinErrorMessage {
  message: string;
  fieldMessages: string[];
}

export interface ValidationErrorResponse {
  message: string;
  errors: FieldValidationError[];
}

export interface FieldValidationError {
  field: string;
  message: string;
}

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
