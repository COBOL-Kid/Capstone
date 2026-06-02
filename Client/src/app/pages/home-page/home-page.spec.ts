import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AddVehicleModalComponent } from '../../components/add-vehicle-modal/add-vehicle-modal';
import { HomePageComponent } from './home-page';
import { VinService } from '../../core/vin/vin.service';
import { AddVinResponse } from '../../core/vin/vin.models';
import { UserVehiclesStore } from '../../core/vin/user-vehicles.store';

describe('HomePageComponent', () => {
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

  function createFixture(addVehicle = vi.fn().mockReturnValue(of(sampleAddResponse))) {
    const vinService = {
      getUserVehicles: vi.fn().mockReturnValue(of([])),
      addVehicle,
    };

    TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [provideRouter([]), { provide: VinService, useValue: vinService }],
    });

    TestBed.inject(UserVehiclesStore).reset();
    const fixture = TestBed.createComponent(HomePageComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigate, vinService };
  }

  it('keeps the add modal open and marks onboarding in progress when a vehicle add starts', () => {
    const addVehicle = vi.fn().mockReturnValue(NEVER);
    const { fixture } = createFixture(addVehicle);
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    fixture.detectChanges();

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(true);
    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(true);
    expect(fixture.nativeElement.querySelector('app-add-vehicle-modal')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Adding vehicle');
    expect(
      fixture.nativeElement.querySelector('.hc-vehicle-onboarding-status__loader'),
    ).not.toBeNull();
    expect(addVehicle).toHaveBeenCalledWith({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
  });

  it('does not show the full-screen onboarding overlay while adding a vehicle', () => {
    const { fixture } = createFixture(vi.fn().mockReturnValue(NEVER));
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-add-vehicle-modal')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-vehicle-onboarding-overlay')).toBeNull();
  });

  it('keeps the add modal open when onboarding ends without a successful add', async () => {
    const addVehicle = vi.fn().mockReturnValue(
      throwError(() => ({
        message: 'Unable to add the vehicle. Please try again.',
        fieldMessages: [],
      })),
    );
    const { fixture } = createFixture(addVehicle);
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(true);
    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Unable to add the vehicle');
    expect(fixture.nativeElement.querySelector('app-add-vehicle-modal')).not.toBeNull();
  });

  it('allows submitting a different VIN after a failed add', async () => {
    const addVehicle = vi
      .fn()
      .mockReturnValueOnce(
        throwError(() => ({
          message: 'Unable to add the vehicle. Please try again.',
          fieldMessages: [],
        })),
      )
      .mockReturnValueOnce(of(sampleAddResponse));
    const { fixture, navigate } = createFixture(addVehicle);
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(false);

    const modal = fixture.debugElement.query(By.directive(AddVehicleModalComponent))
      .componentInstance as AddVehicleModalComponent;
    expect(modal['form'].enabled).toBe(true);

    setInputValue(fixture.nativeElement, 'vin', '1hgbh41jxmn109186');
    setInputValue(fixture.nativeElement, 'currentMileage', '2000');
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: '1HGBH41JXMN109186',
      currentMileage: 2000,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(addVehicle).toHaveBeenCalledTimes(2);
    expect(addVehicle).toHaveBeenNthCalledWith(2, {
      vin: '1HGBH41JXMN109186',
      currentMileage: 2000,
    });
    expect(navigate).toHaveBeenCalledWith(['/vehicles', 'JTENU5JR6M5962554']);
  });

  it('keeps the add modal closed when onboarding ends after a successful add', async () => {
    const { fixture } = createFixture();
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(false);
    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(false);
  });

  it('ignores openAddModal while onboarding is in progress', () => {
    const { fixture } = createFixture(vi.fn().mockReturnValue(NEVER));
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    fixture.componentInstance['onAddVehicleRequest']({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    fixture.detectChanges();

    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(true);
    expect(fixture.nativeElement.querySelector('app-add-vehicle-modal')).not.toBeNull();
  });

  it('cancels an in-flight add when a new add request is submitted', () => {
    const addVehicle = vi.fn().mockReturnValue(NEVER);
    const { fixture } = createFixture(addVehicle);
    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    const firstRequest = { vin: 'JTENU5JR6M5962554', currentMileage: 1000 };
    const secondRequest = { vin: '1HGBH41JXMN109186', currentMileage: 2000 };
    fixture.componentInstance['onAddVehicleRequest'](firstRequest);
    fixture.componentInstance['onAddVehicleRequest'](secondRequest);
    fixture.detectChanges();

    expect(addVehicle).toHaveBeenCalledTimes(2);
    expect(addVehicle).toHaveBeenLastCalledWith(secondRequest);
  });

  it('navigates to vehicle detail when a vehicle is added', async () => {
    const { fixture, navigate } = createFixture();

    fixture.componentInstance['onVehicleAdded'](sampleAddResponse);

    expect(navigate).toHaveBeenCalledWith(['/vehicles', 'JTENU5JR6M5962554']);
    expect(fixture.componentInstance['isAddModalOpen']()).toBe(false);
  });

  it('reflects selected photo updates from the shared vehicles store', async () => {
    const vehicles = [
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
    const vinService = {
      getUserVehicles: vi.fn().mockReturnValue(of(vehicles)),
      addVehicle: vi.fn().mockReturnValue(of(sampleAddResponse)),
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [provideRouter([]), { provide: VinService, useValue: vinService }],
    }).compileComponents();

    const store = TestBed.inject(UserVehiclesStore);
    store.reset();

    const fixture = TestBed.createComponent(HomePageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    store.updateSelectedPhoto('JTENU5JR6M5962554', 'https://example.com/photo-2.jpg');
    fixture.detectChanges();

    expect(fixture.componentInstance['vehicles']()[0].selectedImageUrl).toBe(
      'https://example.com/photo-2.jpg',
    );

    const cardImage = fixture.nativeElement.querySelector(
      '.vehicle-card__image',
    ) as HTMLImageElement;
    expect(cardImage.src).toBe('https://example.com/photo-2.jpg');
  });
});

function setInputValue(host: HTMLElement, controlName: string, value: string): void {
  const input = host.querySelector(`[formControlName="${controlName}"]`) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}
