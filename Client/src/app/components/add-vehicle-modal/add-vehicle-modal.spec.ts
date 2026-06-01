import { NEVER, of, throwError } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { AddVehicleModalComponent } from './add-vehicle-modal';
import { VinService } from '../../core/vin/vin.service';
import { AddVinResponse } from '../../core/vin/vin.models';

describe('AddVehicleModalComponent', () => {
  function configure(vinService = { addVehicle: vi.fn() }) {
    TestBed.configureTestingModule({
      imports: [AddVehicleModalComponent],
      providers: [{ provide: VinService, useValue: vinService }],
    });

    return vinService;
  }

  it('blocks invalid VIN submissions and displays validation messages', () => {
    const vinService = configure();
    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'too-short');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(vinService.addVehicle).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'VIN must be 17 characters and cannot contain I, O, or Q',
    );
  });

  it('renders VIN-not-found errors returned from the service', () => {
    const vinService = configure();
    vinService.addVehicle.mockReturnValue(
      throwError(() => ({
        message: "We couldn't find a vehicle for that VIN. Check the number and try again.",
        fieldMessages: [],
      })),
    );

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'N0TF0UNDV1N123456');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(vinService.addVehicle).toHaveBeenCalledWith({
      vin: 'N0TF0UNDV1N123456',
      currentMileage: 1000,
    });
    expect(fixture.nativeElement.textContent).toContain(
      "We couldn't find a vehicle for that VIN. Check the number and try again.",
    );
  });

  it('renders onboarding-unavailable errors returned from the service', () => {
    const vinService = configure();
    vinService.addVehicle.mockReturnValue(
      throwError(() => ({
        message:
          "We couldn't load vehicle data for this vehicle right now. Please try again later.",
        fieldMessages: [],
      })),
    );

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'JTENU5JR6M5962554');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      "We couldn't load vehicle data for this vehicle right now. Please try again later.",
    );
  });

  it('emits submittingChange true then false on success', () => {
    const vinService = configure();
    const response: AddVinResponse = {
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
    vinService.addVehicle.mockReturnValue(of(response));

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    const submittingStates: boolean[] = [];
    fixture.componentInstance.submittingChange.subscribe((active) => submittingStates.push(active));
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'JTENU5JR6M5962554');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(submittingStates).toEqual([true, false]);
  });

  it('does not emit submittingChange for invalid submissions', () => {
    const vinService = configure();
    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    const submittingStates: boolean[] = [];
    fixture.componentInstance.submittingChange.subscribe((active) => submittingStates.push(active));
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'too-short');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(submittingStates).toEqual([]);
  });

  it('does not close while submitting via escape, backdrop, or close button', () => {
    const vinService = configure();
    vinService.addVehicle.mockReturnValue(NEVER);

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    const closeEvents: void[] = [];
    fixture.componentInstance.close.subscribe(() => closeEvents.push());
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'JTENU5JR6M5962554');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    const backdrop = fixture.nativeElement.querySelector('.hc-overlay-backdrop') as HTMLElement;
    backdrop.click();
    fixture.detectChanges();

    const closeButton = fixture.nativeElement.querySelector(
      '.hc-dialog__close',
    ) as HTMLButtonElement;
    closeButton.click();
    fixture.detectChanges();

    expect(closeEvents).toEqual([]);
  });

  it('submits a valid request and emits the added vehicle', () => {
    const vinService = configure();
    const response: AddVinResponse = {
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
    vinService.addVehicle.mockReturnValue(of(response));

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    const addedVehicles: AddVinResponse[] = [];
    fixture.componentInstance.added.subscribe((vehicle) => addedVehicles.push(vehicle));
    fixture.detectChanges();

    setInputValue(fixture.nativeElement, 'vin', 'jtenu5jr6m5962554');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));

    expect(vinService.addVehicle).toHaveBeenCalledWith({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 1000,
    });
    expect(addedVehicles).toEqual([response]);
  });
});

function setInputValue(host: HTMLElement, controlName: string, value: string): void {
  const input = host.querySelector(`[formControlName="${controlName}"]`) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}
