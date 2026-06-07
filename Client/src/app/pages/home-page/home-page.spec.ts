import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AddVehicleModalComponent } from '../../components/add-vehicle-modal/add-vehicle-modal';
import { AccountDetails } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
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

  function accountDetails(emailVerified: boolean): AccountDetails {
    return {
      userId: 1,
      email: 'pat@example.com',
      firstName: 'Pat',
      lastName: 'Driver',
      userSms: null,
      emailVerified,
      emailVerifiedAt: emailVerified ? '2026-01-02T03:04:00Z' : null,
      createdAt: '2026-01-02T03:04:00Z',
      updatedAt: '2026-02-03T04:05:00Z',
    };
  }

  function createAuthService(emailVerified: boolean | null = true) {
    const account = signal<AccountDetails | null>(
      emailVerified === null ? null : accountDetails(emailVerified),
    );

    return {
      account,
      isEmailVerified: computed(() => account()?.emailVerified === true),
      getCurrentAccount: vi
        .fn()
        .mockReturnValue(emailVerified === null ? NEVER : of(accountDetails(emailVerified))),
      verifyEmailCode: vi.fn().mockReturnValue(of({ token: 'verified-token' })),
      resendEmailVerification: vi.fn().mockReturnValue(of({ emailVerified: false })),
    };
  }

  function createFixture(
    addVehicle = vi
      .fn()
      .mockReturnValue(of({ kind: 'completed' as const, response: sampleAddResponse })),
    emailVerified: boolean | null = true,
  ) {
    const vinService = {
      getUserVehicles: vi.fn().mockReturnValue(of([])),
      addVehicle,
    };
    const authService = createAuthService(emailVerified);

    TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        provideRouter([]),
        { provide: VinService, useValue: vinService },
        { provide: AuthService, useValue: authService },
      ],
    });

    TestBed.inject(UserVehiclesStore).reset();
    const fixture = TestBed.createComponent(HomePageComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigate, vinService, authService };
  }

  it('shows a verification banner and disables add controls for unverified users', () => {
    const { fixture } = createFixture(undefined, false);

    expect(fixture.nativeElement.textContent).toContain('Verify your email');
    const addButton = fixture.nativeElement.querySelector('.home__add-button') as HTMLButtonElement;
    const emptyAddButton = fixture.nativeElement.querySelector(
      '.home__empty .hc-btn',
    ) as HTMLButtonElement;
    expect(addButton?.disabled ?? true).toBe(true);
    expect(emptyAddButton?.disabled ?? true).toBe(true);
  });

  it('does not show the verification banner or disable add controls before account state loads', async () => {
    const { fixture } = createFixture(undefined, null);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Verify your email');
    const emptyAddButton = fixture.nativeElement.querySelector(
      '.home__empty .hc-btn',
    ) as HTMLButtonElement;
    expect(emptyAddButton.disabled).toBe(false);
  });

  it('does not open the add modal when email verification is missing', () => {
    const { fixture } = createFixture(undefined, false);

    fixture.componentInstance['openAddModal']();
    fixture.detectChanges();

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(false);
  });

  it('refreshes account state after successful email verification', async () => {
    const { fixture, authService } = createFixture(undefined, false);
    fixture.detectChanges();

    fixture.componentInstance['openVerificationPanel']();
    fixture.detectChanges();

    fixture.componentInstance['submitVerificationCode']('123456');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(authService.verifyEmailCode).toHaveBeenCalledWith('123456');
    expect(authService.getCurrentAccount).toHaveBeenCalledWith({ forceRefresh: true });
    expect(fixture.componentInstance['isVerificationPanelOpen']()).toBe(false);
  });

  it('keeps the add modal open with trim context when trim selection is required', async () => {
    const trimContext = {
      requiresTrimSelection: true as const,
      year: '2021',
      make: 'Toyota',
      model: '4RUNNER',
    };
    const addVehicle = vi
      .fn()
      .mockReturnValue(of({ kind: 'trimSelectionRequired' as const, context: trimContext }));
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

    expect(fixture.componentInstance['isAddModalOpen']()).toBe(true);
    expect(fixture.componentInstance['isOnboardingVehicle']()).toBe(false);
    expect(fixture.componentInstance['trimSelectionContext']()).toEqual(trimContext);
    expect(navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Full vehicle details may not be available',
    );
  });

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
    expect(fixture.nativeElement.textContent).toContain('this may take a while…');
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
      .mockReturnValueOnce(of({ kind: 'completed' as const, response: sampleAddResponse }));
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

    const authService = createAuthService(true);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        provideRouter([]),
        { provide: VinService, useValue: vinService },
        { provide: AuthService, useValue: authService },
      ],
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
