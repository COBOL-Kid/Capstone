import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { apiConfig } from '../api/api.config';
import { CompleteRecallRequest, CompletedRecallResponse } from './recall.models';

@Injectable({
  providedIn: 'root',
})
export class RecallService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = apiConfig.recallUrl;

  completeRecall(request: CompleteRecallRequest): Observable<CompletedRecallResponse> {
    return this.http.post<CompletedRecallResponse>(`${this.baseUrl}/completed`, request);
  }

  uncompleteRecall(completedRecallId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/completed/${completedRecallId}`);
  }
}
