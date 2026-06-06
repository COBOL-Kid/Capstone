import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, finalize, Subject, switchMap } from 'rxjs';
import { Router, RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../core/auth/auth.service';
import { AuthErrorMessage } from '../../core/auth/auth.models';
import { VinService } from '../../core/vin/vin.service';
import {
  AddVinRequest,
  AddVinResponse,
  AddVinTrimSelectionRequiredResponse,
  UserVehicleResponse,
  VinErrorMessage,
} from '../../core/vin/vin.models';
import { UserVehiclesStore } from '../../core/vin/user-vehicles.store';
import { AddVehicleModalComponent } from '../../components/add-vehicle-modal/add-vehicle-modal';
import { DeleteVehicleModalComponent } from '../../components/delete-vehicle-modal/delete-vehicle-modal';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [AddVehicleModalComponent, DeleteVehicleModalComponent, ReactiveFormsModule, RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePageComponent {
  private readonly vinService = inject(VinService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly vehiclesStore = inject(UserVehiclesStore);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(NonNullableFormBuilder);

  private readonly vehiclesResource = rxResource({
    stream: () => this.vinService.getUserVehicles(),
    defaultValue: [] as UserVehicleResponse[],
  });

  private vehicleAddSucceeded = false;
  private readonly addVehicleRequests = new Subject<AddVinRequest>();

  protected readonly vehicles = this.vehiclesStore.vehicles;
  protected readonly isAddModalOpen = signal(false);
  protected readonly isOnboardingVehicle = signal(false);
  protected readonly addVehicleError = signal<VinErrorMessage | null>(null);
  protected readonly trimSelectionContext = signal<AddVinTrimSelectionRequiredResponse | null>(
    null,
  );
  protected readonly vehicleToDelete = signal<string | null>(null);
  protected readonly isVerificationPanelOpen = signal(false);
  protected readonly isVerifyingEmail = signal(false);
  protected readonly isResendingVerification = signal(false);
  protected readonly verificationError = signal<AuthErrorMessage | null>(null);
  protected readonly verificationForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });
  protected readonly isEmailVerificationRequired = computed(
    () => this.authService.account()?.emailVerified === false,
  );
  protected readonly canAddVehicle = computed(
    () => !this.isEmailVerificationRequired() && !this.isOnboardingVehicle(),
  );
  protected readonly isLoading = computed(() => this.vehiclesResource.isLoading());
  protected readonly error = computed(() => {
    const err = this.vehiclesResource.error();
    if (!err) {
      return null;
    }
    return 'Failed to load vehicles.';
  });

  constructor() {
    this.authService
      .getCurrentAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: () => undefined });

    effect(() => {
      if (this.vehiclesResource.error()) {
        return;
      }
      this.vehiclesStore.setVehicles(this.vehiclesResource.value());
    });

    this.addVehicleRequests
      .pipe(
        switchMap((request) => {
          this.vehicleAddSucceeded = false;
          this.addVehicleError.set(null);
          this.isOnboardingVehicle.set(true);

          return this.vinService.addVehicle(request).pipe(
            finalize(() => this.isOnboardingVehicle.set(false)),
            catchError((error: VinErrorMessage) => {
              this.isOnboardingVehicle.set(false);
              this.addVehicleError.set(error);
              if (!this.vehicleAddSucceeded) {
                this.isAddModalOpen.set(true);
              }
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (result) => {
          if (result.kind === 'trimSelectionRequired') {
            this.trimSelectionContext.set(result.context);
            this.isAddModalOpen.set(true);
            return;
          }
          this.trimSelectionContext.set(null);
          this.onVehicleAdded(result.response);
        },
      });
  }

  protected openVerificationPanel(): void {
    this.verificationError.set(null);
    this.isVerificationPanelOpen.set(true);
  }

  protected closeVerificationPanel(): void {
    this.isVerificationPanelOpen.set(false);
    this.verificationError.set(null);
    this.verificationForm.reset();
  }

  protected resendVerificationEmail(): void {
    if (this.isResendingVerification()) {
      return;
    }

    this.verificationError.set(null);
    this.isResendingVerification.set(true);
    this.authService
      .resendEmailVerification()
      .pipe(
        finalize(() => this.isResendingVerification.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (error: AuthErrorMessage) => this.verificationError.set(error),
      });
  }

  protected submitVerificationCode(): void {
    if (this.isVerifyingEmail()) {
      return;
    }

    if (this.verificationForm.invalid) {
      this.verificationForm.markAllAsTouched();
      return;
    }

    this.verificationError.set(null);
    this.isVerifyingEmail.set(true);
    this.authService
      .verifyEmailCode(this.verificationForm.controls.code.value)
      .pipe(
        switchMap(() => this.authService.getCurrentAccount({ forceRefresh: true })),
        finalize(() => this.isVerifyingEmail.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.closeVerificationPanel();
        },
        error: (error: AuthErrorMessage) => this.verificationError.set(error),
      });
  }

  protected openAddModal(): void {
    if (!this.canAddVehicle()) {
      return;
    }
    this.addVehicleError.set(null);
    this.trimSelectionContext.set(null);
    this.isAddModalOpen.set(true);
  }

  protected clearAddVehicleError(): void {
    this.addVehicleError.set(null);
  }

  protected closeAddModal(): void {
    if (this.isOnboardingVehicle()) {
      return;
    }
    this.isAddModalOpen.set(false);
    this.addVehicleError.set(null);
    this.trimSelectionContext.set(null);
  }

  protected onAddVehicleRequest(request: AddVinRequest): void {
    this.addVehicleRequests.next(request);
  }

  protected onVehicleAdded(response: AddVinResponse): void {
    this.vehicleAddSucceeded = true;
    this.isAddModalOpen.set(false);
    this.addVehicleError.set(null);
    this.trimSelectionContext.set(null);
    void this.router.navigate(['/vehicles', response.vin]);
  }

  protected openDeleteModal(vin: string): void {
    if (this.isOnboardingVehicle()) {
      return;
    }
    this.vehicleToDelete.set(vin);
  }

  protected closeDeleteModal(): void {
    this.vehicleToDelete.set(null);
  }

  protected onVehicleDeleted(vin: string): void {
    this.vehiclesStore.removeVehicle(vin);
  }
}
