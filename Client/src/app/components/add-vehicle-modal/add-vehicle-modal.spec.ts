import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { AddVehicleModalComponent } from './add-vehicle-modal';
import { AddVinRequest } from '../../core/vin/vin.models';

describe('AddVehicleModalComponent', () => {
  function createFixture(
    options: {
      serverError?: { message: string; fieldMessages: string[] } | null;
      isSubmitting?: boolean;
    } = {},
  ) {
    TestBed.configureTestingModule({
      imports: [AddVehicleModalComponent],
    });

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    if (options.serverError) {
      fixture.componentRef.setInput('serverError', options.serverError);
    }
    if (options.isSubmitting) {
      fixture.componentRef.setInput('isSubmitting', options.isSubmitting);
    }
    fixture.detectChanges();
    return fixture;
  }

  it('blocks invalid VIN submissions and displays validation messages', () => {
    const fixture = createFixture();
    const requests: AddVinRequest[] = [];
    fixture.componentInstance.submitRequest.subscribe((request) => requests.push(request));

    setInputValue(fixture.nativeElement, 'vin', 'too-short');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(requests).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain(
      'VIN must be 17 characters and cannot contain I, O, or Q',
    );
  });

  it('renders server errors passed from the parent', () => {
    const fixture = createFixture({
      serverError: {
        message:
          "We don't have vehicle information for this VIN in our system yet. Try a different VIN or check back later as we add more vehicles.",
        fieldMessages: [],
      },
    });

    expect(fixture.nativeElement.textContent).toContain(
      "We don't have vehicle information for this VIN in our system yet",
    );
  });

  it('emits submitRequest for a valid form submission', () => {
    const fixture = createFixture();
    const requests: AddVinRequest[] = [];
    fixture.componentInstance.submitRequest.subscribe((request) => requests.push(request));

    setInputValue(fixture.nativeElement, 'vin', 'jtenu5jr6m5962554');
    setInputValue(fixture.nativeElement, 'currentMileage', '1000');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(requests).toEqual([
      {
        vin: 'JTENU5JR6M5962554',
        currentMileage: 1000,
      },
    ]);
  });

  it('disables inputs and submit while submitting', () => {
    const fixture = createFixture({ isSubmitting: true });

    const submitButton = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;

    expect(fixture.componentInstance['form'].disabled).toBe(true);
    expect(submitButton.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Adding vehicle');
    expect(
      fixture.nativeElement.querySelector('.hc-vehicle-onboarding-status__loader'),
    ).not.toBeNull();
  });

  it('emits serverErrorClear when the user edits the VIN after an error', () => {
    const fixture = createFixture({
      serverError: {
        message: 'Unable to add the vehicle. Please try again.',
        fieldMessages: [],
      },
    });
    const clearSpy = vi.fn();
    fixture.componentInstance.serverErrorClear.subscribe(clearSpy);

    setInputValue(fixture.nativeElement, 'vin', '1HGBH41JXMN109186');
    fixture.detectChanges();

    expect(clearSpy).toHaveBeenCalled();
  });
});

function setInputValue(host: HTMLElement, controlName: string, value: string): void {
  const input = host.querySelector(`[formControlName="${controlName}"]`) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}
