import { TestBed } from '@angular/core/testing';

import { UserVehiclesStore } from './user-vehicles.store';
import { UserVehicleResponse } from './vin.models';

describe('UserVehiclesStore', () => {
  let store: UserVehiclesStore;

  const sampleVehicles: UserVehicleResponse[] = [
    {
      vin: 'JTENU5JR6M5962554',
      currentMileage: 45000,
      vehicleTypeId: 7,
      make: 'Toyota',
      model: '4RUNNER',
      trim: 'SRS Prem',
      year: '2021',
      availableImageUrls: ['https://example.com/photo-1.jpg', 'https://example.com/photo-2.jpg'],
      selectedImageUrl: 'https://example.com/photo-1.jpg',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(UserVehiclesStore);
    store.reset();
  });

  it('returns empty list before vehicles are loaded', () => {
    expect(store.vehicles()).toEqual([]);
  });

  it('updates selected photo for a vehicle', () => {
    store.setVehicles(sampleVehicles);

    store.updateSelectedPhoto('JTENU5JR6M5962554', 'https://example.com/photo-2.jpg');

    expect(store.vehicles()[0].selectedImageUrl).toBe('https://example.com/photo-2.jpg');
  });

  it('removes a vehicle from the cached list', () => {
    store.setVehicles(sampleVehicles);

    store.removeVehicle('JTENU5JR6M5962554');

    expect(store.vehicles()).toEqual([]);
  });

  it('clears cached vehicles on reset', () => {
    store.setVehicles(sampleVehicles);

    store.reset();

    expect(store.vehicles()).toEqual([]);
  });
});
