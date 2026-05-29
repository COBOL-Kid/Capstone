export interface PartCostResponse {
  partDesc: string;
  totalCost: number;
  currency: string;
}

export interface LaborCostResponse {
  timeRequiredHours: number;
  hourlyRate: number;
  totalCost: number;
  currency: string;
}

export interface MileageCostSummaryResponse {
  totalPartsCost: number;
  totalLaborCost: number;
  totalCost: number;
  currency: string;
}

export interface UpcomingMaintenanceItemResponse {
  maintMileageId: number;
  maintDesc: string;
  isInspect: boolean;
  labor: LaborCostResponse | null;
  parts: PartCostResponse[];
}

export interface UpcomingMaintenanceIntervalResponse {
  mileageDue: number;
  summary: MileageCostSummaryResponse | null;
  items: UpcomingMaintenanceItemResponse[];
}

/** Used when opening the maintenance detail modal from an upcoming item. */
export interface SelectedUpcomingMaintenance {
  maintMileageId: number;
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

export interface MiscMaintenanceCostResponse {
  miscMaintCostId: number;
  maintTitle: string;
  maintDesc: string | null;
  independentAvg: number | null;
  independentHigh: number | null;
  independentLow: number | null;
  dealerAvg: number | null;
  dealerHigh: number | null;
  dealerLow: number | null;
}
