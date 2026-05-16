import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { VinService } from '../../core/vin/vin.service';
import { UserVehicleResponse } from '../../core/vin/vin.models';
import { AddVehicleModalComponent } from '../../components/add-vehicle-modal/add-vehicle-modal';
import { DeleteVehicleModalComponent } from '../../components/delete-vehicle-modal/delete-vehicle-modal';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [AddVehicleModalComponent, DeleteVehicleModalComponent],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePageComponent {
  protected readonly vehicles = signal<UserVehicleResponse[]>([]);
  protected readonly isAddModalOpen = signal(false);
  protected readonly vehicleToDelete = signal<string | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);
  private readonly vinService = inject(VinService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.loadVehicles();
  }

  protected openAddModal(): void {
    this.isAddModalOpen.set(true);
  }

  protected closeAddModal(): void {
    this.isAddModalOpen.set(false);
  }

  protected onVehicleAdded(): void {
    this.loadVehicles();
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

  private loadVehicles(): void {
    this.isLoading.set(true);
    this.vinService
      .getUserVehicles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (vehicles) => {
          this.vehicles.set(vehicles || []);
          this.isLoading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          if (
            err.status === 204 ||
            err.status === 404 ||
            err.error instanceof SyntaxError ||
            err.message?.includes('parse')
          ) {
            this.vehicles.set([]);
            this.error.set(null);
          } else {
            this.error.set('Failed to load vehicles.');
          }
          this.isLoading.set(false);
        },
      });
  }
}
