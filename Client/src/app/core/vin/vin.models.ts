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
