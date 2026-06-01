import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { HomePageComponent } from './home-page';
import { VinService } from '../../core/vin/vin.service';
import { AddVinResponse } from '../../core/vin/vin.models';

describe('HomePageComponent', () => {
  function createFixture() {
    const navigate = vi.fn().mockResolvedValue(true);
    const vinService = {
      getUserVehicles: vi.fn().mockReturnValue(of([])),
    };

    TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        { provide: VinService, useValue: vinService },
        { provide: Router, useValue: { navigate } },
      ],
    });

    const fixture = TestBed.createComponent(HomePageComponent);
    fixture.detectChanges();
    return { fixture, navigate };
  }

  const sampleAddResponse: AddVinResponse = {
    vin: 'JTENU5JR6M5962554',
    currentMileage: 1000,
    vehicleTypeId: 7,
    make: 'Toyota',
    model: '4RUNNER',
    trim: 'SRS Prem',
    year: '2021',
    availableImageUrls: [],
    selectedImageUrl: '',
    createdVin: true,
    createdVehicleType: false,
    createdAssociation: true,
  };

  it('closes the add modal when onboarding starts', () => {
    const { fixture } = createFixture();
    fixture.componentInstance['openAddModal']();

    fixture.componentInstance['onOnboardingChange'](true);

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(false);
    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(true);
  });

  it('reopens the add modal when onboarding ends without a successful add', () => {
    const { fixture } = createFixture();
    fixture.componentInstance['onOnboardingChange'](true);

    fixture.componentInstance['onOnboardingChange'](false);

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(true);
    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(false);
  });

  it('keeps the add modal closed when onboarding ends after a successful add', () => {
    const { fixture } = createFixture();
    fixture.componentInstance['onOnboardingChange'](true);
    fixture.componentInstance['onVehicleAdded'](sampleAddResponse);

    fixture.componentInstance['onOnboardingChange'](false);

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(false);
  });

  it('navigates to vehicle detail when a vehicle is added', () => {
    const { fixture, navigate } = createFixture();

    fixture.componentInstance['onVehicleAdded'](sampleAddResponse);

    expect(navigate).toHaveBeenCalledWith(['/vehicles', 'JTENU5JR6M5962554']);
    expect(fixture.componentInstance['isAddModalOpen']()).toBe(false);
  });
});
