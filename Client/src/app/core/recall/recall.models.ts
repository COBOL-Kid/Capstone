export interface RecallResponse {
  recallId: number;
  vin: string;
  nhtsaCampaignNumber: string;
  reportReceivedDate: string;
  component: string;
  summary: string;
  consequence: string;
  remedy: string;
}

export interface CompletedRecallResponse {
  completedRecallId: number;
  vin: string;
  recallId: number;
  completedDate: string;
  repairShop: string | null;
  cost: number | null;
  notes: string | null;
  nhtsaCampaignNumber: string;
  reportReceivedDate: string;
  component: string;
  summary: string;
  consequence: string;
  remedy: string;
}

export interface CompleteRecallRequest {
  vin: string;
  recallId: number;
  completedDate?: string;
  repairShop?: string | null;
  cost?: number | null;
  notes?: string | null;
}
