import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { apiConfig } from '../api/api.config';
import { CompleteRecallRequest, CompletedRecallResponse, RecallResponse } from './recall.models';

@Injectable({
  providedIn: 'root',
})
export class RecallService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = apiConfig.recallUrl;

  getUncompleted(vin: string): Observable<RecallResponse[]> {
    return this.http
      .get<RecallResponse[]>(`${this.baseUrl}/${vin}/uncompleted`)
      .pipe(map((items) => items ?? []));
  }

  getCompleted(vin: string): Observable<CompletedRecallResponse[]> {
    return this.http
      .get<CompletedRecallResponse[]>(`${this.baseUrl}/${vin}/completed`)
      .pipe(map((items) => items ?? []));
  }

  completeRecall(request: CompleteRecallRequest): Observable<CompletedRecallResponse> {
    return this.http.post<CompletedRecallResponse>(`${this.baseUrl}/completed`, request);
  }

  uncompleteRecall(completedRecallId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/completed/${completedRecallId}`);
  }
}
