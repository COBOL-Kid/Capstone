import { describe, expect, it } from 'vitest';

import type { VehicleDashboardResponse } from '$lib/models/vin';
import { toVehiclePageData } from './vehicle-page';

describe('toVehiclePageData', () => {
  it('renames dashboard fields to page shape', () => {
    const dashboard = {
      detail: { vin: 'VIN123' },
      upcomingMaintenance: [1],
      completedMaintenance: [2],
      uncompletedRecalls: [3],
      completedRecalls: [4],
      miscMaintenanceCosts: [5],
      vehicleWarranty: null,
    } as unknown as VehicleDashboardResponse;

    expect(toVehiclePageData(dashboard)).toEqual({
      detail: { vin: 'VIN123' },
      upcomingIntervals: [1],
      completedMaintenance: [2],
      uncompletedRecalls: [3],
      completedRecalls: [4],
      miscMaintenanceCosts: [5],
      vehicleWarranty: null,
    });
  });
});
