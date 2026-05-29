import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { VinService } from './vin.service';
import { VehicleDashboardResponse } from './vin.models';
import { VehiclePageData } from './vehicle-page.models';

@Injectable({
  providedIn: 'root',
})
export class VehiclePageDataService {
  private readonly vinService = inject(VinService);

  loadVehiclePage(vin: string): Observable<VehiclePageData> {
    return this.vinService
      .getVehicleDashboard(vin)
      .pipe(map((dashboard) => this.toVehiclePageData(dashboard)));
  }

  private toVehiclePageData(dashboard: VehicleDashboardResponse): VehiclePageData {
    return {
      detail: dashboard.detail,
      upcomingIntervals: dashboard.upcomingMaintenance,
      completedMaintenance: dashboard.completedMaintenance,
      uncompletedRecalls: dashboard.uncompletedRecalls,
      completedRecalls: dashboard.completedRecalls,
      miscMaintenanceCosts: dashboard.miscMaintenanceCosts,
    };
  }
}
