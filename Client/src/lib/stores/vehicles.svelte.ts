import type { UserVehicleResponse } from '$lib/models/vin';

class VehiclesStore {
  private vehiclesState = $state<UserVehicleResponse[] | null>(null);

  readonly vehicles = $derived(this.vehiclesState ?? []);

  setVehicles(vehicles: UserVehicleResponse[]): void {
    this.vehiclesState = vehicles;
  }

  updateSelectedPhoto(vin: string, selectedImageUrl: string): void {
    const current = this.vehiclesState;
    if (!current) {
      return;
    }
    this.vehiclesState = current.map((vehicle) =>
      vehicle.vin === vin ? { ...vehicle, selectedImageUrl } : vehicle,
    );
  }

  removeVehicle(vin: string): void {
    const current = this.vehiclesState;
    if (!current) {
      return;
    }
    this.vehiclesState = current.filter((vehicle) => vehicle.vin !== vin);
  }

  reset(): void {
    this.vehiclesState = null;
  }
}

export const vehiclesStore = new VehiclesStore();
