import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { apiConfig } from '../api/api.config';
import {
  CompleteMaintenanceRequest,
  CompletedMaintenanceResponse,
  MaintenanceCostResponse,
  UpcomingMaintenanceResponse,
} from './maintenance.models';

@Injectable({
  providedIn: 'root',
})
export class MaintenanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = apiConfig.maintenanceUrl;

  getUpcoming(vin: string): Observable<UpcomingMaintenanceResponse[]> {
    return this.http
      .get<UpcomingMaintenanceResponse[]>(`${this.baseUrl}/${vin}/upcoming`)
      .pipe(map((items) => items ?? []));
  }

  getCompleted(vin: string): Observable<CompletedMaintenanceResponse[]> {
    return this.http
      .get<CompletedMaintenanceResponse[]>(`${this.baseUrl}/${vin}/completed`)
      .pipe(map((items) => items ?? []));
  }

  getMaintenanceCosts(vin: string): Observable<MaintenanceCostResponse[]> {
    return this.http
      .get<MaintenanceCostResponse[]>(`${this.baseUrl}/${vin}/costs`)
      .pipe(map((items) => items ?? []));
  }

  completeMaintenance(
    request: CompleteMaintenanceRequest,
  ): Observable<CompletedMaintenanceResponse> {
    return this.http.post<CompletedMaintenanceResponse>(`${this.baseUrl}/completed`, request);
  }

  uncompleteMaintenance(completedMaintenanceId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/completed/${completedMaintenanceId}`);
  }
}
