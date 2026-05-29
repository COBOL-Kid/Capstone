import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';

import { VinService } from '../../core/vin/vin.service';
import { UserVehicleResponse } from '../../core/vin/vin.models';
import { AddVehicleModalComponent } from '../../components/add-vehicle-modal/add-vehicle-modal';
import { DeleteVehicleModalComponent } from '../../components/delete-vehicle-modal/delete-vehicle-modal';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [AddVehicleModalComponent, DeleteVehicleModalComponent, RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePageComponent {
  private readonly vinService = inject(VinService);

  private readonly vehiclesResource = rxResource({
    stream: () => this.vinService.getUserVehicles(),
    defaultValue: [] as UserVehicleResponse[],
  });

  protected readonly vehicles = signal<UserVehicleResponse[]>([]);
  protected readonly isAddModalOpen = signal(false);
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

  protected onVehicleAdded(): void {
    this.vehiclesResource.reload();
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
