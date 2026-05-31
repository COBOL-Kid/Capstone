export interface WarrantyCoverageResponse {
  coverageName: string;
  coverageValue: string;
  estimatedExpirationDate: string | null;
  expired: boolean;
  remainingMonths: number | null;
  remainingMiles: number | null;
}

export interface VehicleWarrantyResponse {
  vehicleYear: string;
  vehicleMake: string;
  vehicleModel: string;
  coverages: WarrantyCoverageResponse[];
}
