import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { apiConfig } from '../api/api.config';
import { MaintenanceService } from './maintenance.service';

describe('MaintenanceService', () => {
  let service: MaintenanceService;
  let httpTesting: HttpTestingController;
  const vin = 'JTENU5JR6M5962554';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(MaintenanceService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('returns empty arrays for 204 list responses', () => {
    service.getUpcoming(vin).subscribe((items) => expect(items).toEqual([]));
    httpTesting.expectOne(`${apiConfig.maintenanceUrl}/${vin}/upcoming`).flush(null, {
      status: 204,
      statusText: 'No Content',
    });

    service.getCompleted(vin).subscribe((items) => expect(items).toEqual([]));
    httpTesting.expectOne(`${apiConfig.maintenanceUrl}/${vin}/completed`).flush(null, {
      status: 204,
      statusText: 'No Content',
    });
  });

  it('posts completed maintenance and deletes completion records', () => {
    const completed = {
      completedMaintenanceId: 99,
      vin,
      maintMileageId: 11,
      completedDate: '2025-04-05',
      mileageCompleted: 45100,
      cost: 120.5,
      notes: 'Done',
      maintDesc: 'Oil change',
      mileageDue: 50000,
    };

    service
      .completeMaintenance({ vin, maintMileageId: 11, mileageCompleted: 45100 })
      .subscribe((response) => expect(response).toEqual(completed));

    const postRequest = httpTesting.expectOne(`${apiConfig.maintenanceUrl}/completed`);
    expect(postRequest.request.method).toBe('POST');
    postRequest.flush(completed);

    service.uncompleteMaintenance(99).subscribe();
    const deleteRequest = httpTesting.expectOne(`${apiConfig.maintenanceUrl}/completed/99`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null, { status: 204, statusText: 'No Content' });
  });
});
