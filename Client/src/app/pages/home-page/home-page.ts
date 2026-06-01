import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';

import { VinService } from '../../core/vin/vin.service';
import { AddVinResponse, UserVehicleResponse } from '../../core/vin/vin.models';
import { AddVehicleModalComponent } from '../../components/add-vehicle-modal/add-vehicle-modal';
import { DeleteVehicleModalComponent } from '../../components/delete-vehicle-modal/delete-vehicle-modal';
import { VehicleOnboardingOverlayComponent } from '../../components/vehicle-onboarding-overlay/vehicle-onboarding-overlay';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    AddVehicleModalComponent,
    DeleteVehicleModalComponent,
    RouterLink,
    VehicleOnboardingOverlayComponent,
  ],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePageComponent {
  private readonly vinService = inject(VinService);
  private readonly router = inject(Router);

  private readonly vehiclesResource = rxResource({
    stream: () => this.vinService.getUserVehicles(),
    defaultValue: [] as UserVehicleResponse[],
  });

  private vehicleAddSucceeded = false;

  protected readonly vehicles = signal<UserVehicleResponse[]>([]);
  protected readonly isAddModalOpen = signal(false);
  protected readonly isOnboardingVehicle = signal(false);
  protected readonly vehicleToDelete = signal<string | null>(null);
  protected readonly isLoading = computed(() => this.vehiclesResource.isLoading());
  protected readonly error = computed(() => {
    const err = this.vehiclesResource.error();
    if (!err) {
      return null;
    }
    return 'Failed to load vehicles.';
  });

  constructor() {
    effect(() => {
      if (this.vehiclesResource.error()) {
        return;
      }
      this.vehicles.set(this.vehiclesResource.value());
    });
  }

  protected openAddModal(): void {
    this.isAddModalOpen.set(true);
  }

  protected closeAddModal(): void {
    this.isAddModalOpen.set(false);
  }

  protected onOnboardingChange(active: boolean): void {
    this.isOnboardingVehicle.set(active);
    if (active) {
      this.vehicleAddSucceeded = false;
      this.closeAddModal();
      return;
    }
    if (!this.vehicleAddSucceeded) {
      this.openAddModal();
    }
  }

  protected onVehicleAdded(response: AddVinResponse): void {
    this.vehicleAddSucceeded = true;
    this.closeAddModal();
    void this.router.navigate(['/vehicles', response.vin]);
  }

  protected openDeleteModal(vin: string): void {
    this.vehicleToDelete.set(vin);
  }

  protected closeDeleteModal(): void {
    this.vehicleToDelete.set(null);
  }

  protected onVehicleDeleted(vin: string): void {
    this.vehicles.update((vehicles) => vehicles.filter((v) => v.vin !== vin));
  }
}
