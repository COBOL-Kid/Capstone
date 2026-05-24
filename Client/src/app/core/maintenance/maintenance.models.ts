export interface UpcomingMaintenanceResponse {
  maintMileageId: number;
  vin: string;
  mileageDue: number;
  maintDesc: string;
}

export interface CompletedMaintenanceResponse {
  completedMaintenanceId: number;
  vin: string;
  maintMileageId: number;
  completedDate: string;
  mileageCompleted: number;
  cost: number | null;
  notes: string | null;
  maintDesc: string;
  mileageDue: number;
}

export interface CompleteMaintenanceRequest {
  vin: string;
  maintMileageId: number;
  completedDate?: string;
  mileageCompleted: number;
  cost?: number | null;
  notes?: string | null;
}
