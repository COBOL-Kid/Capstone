import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { apiConfig } from '../api/api.config';
import { VinService } from './vin.service';

describe('VinService', () => {
  let service: VinService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(VinService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('returns empty array for 204 list responses', () => {
    service.getUserVehicles().subscribe((items) => expect(items).toEqual([]));

    httpTesting.expectOne(apiConfig.vinUrl).flush(null, {
      status: 204,
      statusText: 'No Content',
    });
  });

  it('maps 404 responses into a VIN-not-found message', () => {
    service.addVehicle({ vin: 'MISSINGVIN1234567', currentMileage: 1000 }).subscribe({
      error: (error) => {
        expect(error.message).toBe(
          "We couldn't find a vehicle for that VIN. Check the number and try again.",
        );
        expect(error.fieldMessages).toEqual([]);
      },
    });

    const request = httpTesting.expectOne(apiConfig.vinUrl);
    request.flush('Vehicle not found for VIN', { status: 404, statusText: 'Not Found' });
  });

  it('maps 500 plain string responses into an onboarding-unavailable message', () => {
    service.addVehicle({ vin: 'JTENU5JR6M5962554', currentMileage: 1000 }).subscribe({
      error: (error) => {
        expect(error.message).toBe(
          "We couldn't load vehicle data for this vehicle right now. Please try again later.",
        );
        expect(error.fieldMessages).toEqual([]);
      },
    });

    const request = httpTesting.expectOne(apiConfig.vinUrl);
    request.flush("Sometimes things just don't go as planned.", {
      status: 500,
      statusText: 'Internal Server Error',
    });
  });

  it('maps backend validation errors into user-facing field messages', () => {
    service.addVehicle({ vin: 'too-short', currentMileage: -1 }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Validation failed');
        expect(error.fieldMessages).toEqual([
          'VIN must be 17 characters and cannot contain I, O, or Q',
        ]);
      },
    });

    const request = httpTesting.expectOne(apiConfig.vinUrl);
    request.flush(
      {
        message: 'Validation failed',
        errors: [
          {
            field: 'vin',
            message: 'VIN must be 17 characters and cannot contain I, O, or Q',
          },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
  });

  it('loads vehicle dashboard', () => {
    const dashboard = {
      detail: {
        vin: 'JTENU5JR6M5962554',
        vehicleTypeId: 7,
        vehicleMake: 'Toyota',
        vehicleModel: '4RUNNER',
        vehicleTrim: 'SRS Prem',
        vehicleYear: '2021',
        vehicleStyle: 'SUV',
        sourceVin: null,
        origin: null,
        body: null,
        engineDescription: null,
        transmissionStyle: null,
        driveType: null,
        ownersManual: null,
        currentMileage: 45000,
        availableImageUrls: [],
        selectedImageUrl: '',
      },
      upcomingMaintenance: [],
      completedMaintenance: [],
      uncompletedRecalls: [],
      completedRecalls: [],
      miscMaintenanceCosts: [],
      vehicleWarranty: null,
    };

    service.getVehicleDashboard('JTENU5JR6M5962554').subscribe((response) => {
      expect(response).toEqual(dashboard);
    });

    const request = httpTesting.expectOne(`${apiConfig.vinUrl}/JTENU5JR6M5962554/dashboard`);
    request.flush(dashboard);
  });

  it('loads vehicle detail and patches mileage', () => {
    const detail = {
      vin: 'JTENU5JR6M5962554',
      vehicleTypeId: 7,
      vehicleMake: 'Toyota',
      vehicleModel: '4RUNNER',
      vehicleTrim: 'SRS Prem',
      vehicleYear: '2021',
      vehicleStyle: 'SUV',
      sourceVin: null,
      origin: null,
      body: null,
      engineDescription: null,
      transmissionStyle: null,
      driveType: null,
      ownersManual: null,
      currentMileage: 45000,
      availableImageUrls: [],
      selectedImageUrl: '',
    };

    service.getVehicleDetail('JTENU5JR6M5962554').subscribe((response) => {
      expect(response).toEqual(detail);
    });

    const getRequest = httpTesting.expectOne(`${apiConfig.vinUrl}/JTENU5JR6M5962554`);
    getRequest.flush(detail);

    service.updateMileage('JTENU5JR6M5962554', { currentMileage: 52000 }).subscribe((response) => {
      expect(response.currentMileage).toBe(52000);
    });

    const patchRequest = httpTesting.expectOne(`${apiConfig.vinUrl}/JTENU5JR6M5962554/mileage`);
    expect(patchRequest.request.method).toBe('PATCH');
    expect(patchRequest.request.body).toEqual({ currentMileage: 52000 });
    patchRequest.flush({ ...detail, currentMileage: 52000 });
  });

  it('maps plain string 400 responses into user-facing messages', () => {
    service.addVehicle({ vin: 'JTENU5JR6M5962554', currentMileage: 1000 }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Current mileage cannot be negative');
        expect(error.fieldMessages).toEqual([]);
      },
    });

    const request = httpTesting.expectOne(apiConfig.vinUrl);
    request.flush('Current mileage cannot be negative', {
      status: 400,
      statusText: 'Bad Request',
    });
  });
});
