import { TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AddVehicleModalComponent } from './add-vehicle-modal';
import { AddVinRequest, AddVinTrimSelectionRequiredResponse } from '../../core/vin/vin.models';
import { VinService } from '../../core/vin/vin.service';

describe('AddVehicleModalComponent', () => {
  const trimSelectionContext: AddVinTrimSelectionRequiredResponse = {
    requiresTrimSelection: true,
    year: '2021',
    make: 'Toyota',
    model: '4RUNNER',
  };

  function createFixture(
    options: {
      serverError?: { message: string; fieldMessages: string[] } | null;
      isSubmitting?: boolean;
      trimSelectionContext?: AddVinTrimSelectionRequiredResponse | null;
      getTrimOptions?: ReturnType<typeof vi.fn>;
    } = {},
  ) {
    const getTrimOptions =
      options.getTrimOptions ?? vi.fn().mockReturnValue(of(['SRS Prem', 'Limited']));

    TestBed.configureTestingModule({
      imports: [AddVehicleModalComponent],
      providers: [{ provide: VinService, useValue: { getTrimOptions } }],
    });

    const fixture = TestBed.createComponent(AddVehicleModalComponent);
    if (options.trimSelectionContext !== undefined) {
      fixture.componentRef.setInput('trimSelectionContext', options.trimSelectionContext);
    }
    if (options.serverError) {
      fixture.componentRef.setInput('serverError', options.serverError);
    }
    if (options.isSubmitting) {
      fixture.componentRef.setInput('isSubmitting', options.isSubmitting);
    }
    fixture.detectChanges();
    return { fixture, getTrimOptions };
  }

  it('shows limited data warning when trim selection is required', () => {
    const { fixture } = createFixture({ trimSelectionContext });

    expect(fixture.nativeElement.textContent).toContain(
      'Full vehicle details may not be available',
    );
    expect(fixture.nativeElement.textContent).toContain('Continue anyways');
  });

  it('loads trim options and submits selected trim after continuing', async () => {
    const getTrimOptions = vi.fn().mockReturnValue(of(['SRS Prem', 'Limited']));
    const { fixture } = createFixture({ trimSelectionContext, getTrimOptions });
    const requests: AddVinRequest[] = [];
    fixture.componentInstance.submitRequest.subscribe((request) => requests.push(request));
    fixture.componentInstance['form'].setValue({
      vin: 'JTENU5JR6M5962554',
      currentMileage: 45000,
    });

    const continueButton = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button'),
    ).find((button) => button.textContent?.includes('Continue anyways'));
    continueButton?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getTrimOptions).toHaveBeenCalledWith('2021', 'Toyota', '4RUNNER');
    expect(fixture.nativeElement.textContent).toContain('SRS Prem');
    expect(fixture.nativeElement.textContent).toContain('Limited');

    const trimSelect = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    trimSelect.value = 'Limited';
    trimSelect.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const confirmButton = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button'),
    ).find((button) => button.textContent?.trim() === 'Confirm');
    confirmButton?.click();
    fixture.detectChanges();

    expect(requests).toEqual([
      {
        vin: 'JTENU5JR6M5962554',
        currentMileage: 45000,
        selectedTrim: 'Limited',
      },
    ]);
  });

  it('requires a trim selection before confirming', () => {
    const { fixture } = createFixture({ trimSelectionContext });
    fixture.componentInstance['continueDespiteLimitedData']();
    fixture.detectChanges();

    const confirmButton = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button'),
    ).find((button) => button.textContent?.trim() === 'Confirm');
    confirmButton?.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Select a trim to continue.');
  });

  it('does not close on backdrop click while trim options are loading', () => {
    const getTrimOptions = vi.fn().mockReturnValue(NEVER);
    const { fixture } = createFixture({ trimSelectionContext, getTrimOptions });
    const closeSpy = vi.fn();
    fixture.componentInstance.close.subscribe(closeSpy);

    fixture.componentInstance['continueDespiteLimitedData']();
    fixture.detectChanges();

    const backdrop = fixture.nativeElement.querySelector('.hc-overlay-backdrop') as HTMLDivElement;
    backdrop.click();
    fixture.detectChanges();

    expect(closeSpy).not.toHaveBeenCalled();
  });

  it('shows an error when trim options fail to load', async () => {
    const getTrimOptions = vi.fn().mockReturnValue(throwError(() => new Error('network failure')));
    const { fixture } = createFixture({ trimSelectionContext, getTrimOptions });

    fixture.componentInstance['continueDespiteLimitedData']();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Unable to load trim options. Please try again.',
    );
  });

  it('blocks invalid VIN submissions and displays validation messages', () => {
    const { fixture } = createFixture();
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
    const { fixture } = createFixture({
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
    const { fixture } = createFixture();
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
    const { fixture } = createFixture({ isSubmitting: true });

    const submitButton = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;

    expect(fixture.componentInstance['form'].disabled).toBe(true);
    expect(submitButton.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Adding vehicle');
    expect(fixture.nativeElement.textContent).toContain('this may take a while…');
    expect(
      fixture.nativeElement.querySelector('.hc-vehicle-onboarding-status__loader'),
    ).not.toBeNull();
  });

  it('emits serverErrorClear when the user edits the VIN after an error', () => {
    const { fixture } = createFixture({
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
