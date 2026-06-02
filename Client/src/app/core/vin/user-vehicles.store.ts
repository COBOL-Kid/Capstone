import { computed, Injectable, signal } from '@angular/core';

import { UserVehicleResponse } from './vin.models';

@Injectable({
  providedIn: 'root',
})
export class UserVehiclesStore {
  private readonly vehiclesState = signal<UserVehicleResponse[] | null>(null);

  readonly vehicles = computed(() => this.vehiclesState() ?? []);

  setVehicles(vehicles: UserVehicleResponse[]): void {
    this.vehiclesState.set(vehicles);
  }

  updateSelectedPhoto(vin: string, selectedImageUrl: string): void {
    this.vehiclesState.update((vehicles) => {
      if (!vehicles) {
        return vehicles;
      }

      return vehicles.map((vehicle) =>
        vehicle.vin === vin ? { ...vehicle, selectedImageUrl } : vehicle,
      );
    });
  }

  removeVehicle(vin: string): void {
    this.vehiclesState.update((vehicles) => {
      if (!vehicles) {
        return vehicles;
      }

      return vehicles.filter((vehicle) => vehicle.vin !== vin);
    });
  }

  reset(): void {
    this.vehiclesState.set(null);
  }
}
