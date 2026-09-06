import { apiDelete, apiPost } from '$lib/api/client';
import { apiConfig } from '$lib/api/config';
import type { CompleteRecallRequest, CompletedRecallResponse } from '$lib/models/recall';

export function completeRecall(request: CompleteRecallRequest): Promise<CompletedRecallResponse> {
  return apiPost<CompletedRecallResponse>(`${apiConfig.recallUrl}/completed`, request);
}

export function uncompleteRecall(completedRecallId: number): Promise<void> {
  return apiDelete<void>(`${apiConfig.recallUrl}/completed/${completedRecallId}`);
}
