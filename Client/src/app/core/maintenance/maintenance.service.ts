import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { apiConfig } from '../api/api.config';
import { CompleteMaintenanceRequest, CompletedMaintenanceResponse } from './maintenance.models';

@Injectable({
  providedIn: 'root',
})
export class MaintenanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = apiConfig.maintenanceUrl;

  completeMaintenance(
    request: CompleteMaintenanceRequest,
  ): Observable<CompletedMaintenanceResponse> {
    return this.http.post<CompletedMaintenanceResponse>(`${this.baseUrl}/completed`, request);
  }

  uncompleteMaintenance(completedMaintenanceId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/completed/${completedMaintenanceId}`);
  }
}
