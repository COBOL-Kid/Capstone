import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';

import { VinService } from '../../core/vin/vin.service';
import { VehicleDetailResponse } from '../../core/vin/vin.models';
import { MaintenanceService } from '../../core/maintenance/maintenance.service';
import {
  CompletedMaintenanceResponse,
  UpcomingMaintenanceResponse,
} from '../../core/maintenance/maintenance.models';
import { RecallService } from '../../core/recall/recall.service';
import { CompletedRecallResponse, RecallResponse } from '../../core/recall/recall.models';
import { UpdateMileageModalComponent } from '../../components/update-mileage-modal/update-mileage-modal';
import { MaintenanceDetailModalComponent } from '../../components/maintenance-detail-modal/maintenance-detail-modal';
import { RecallDetailModalComponent } from '../../components/recall-detail-modal/recall-detail-modal';

type ActiveSection = 'maintenance' | 'recalls';

@Component({
  selector: 'app-vehicle-detail-page',
  standalone: true,
  imports: [
    DecimalPipe,
    RouterLink,
    UpdateMileageModalComponent,
    MaintenanceDetailModalComponent,
    RecallDetailModalComponent,
  ],
  templateUrl: './vehicle-detail-page.html',
  styleUrl: './vehicle-detail-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehicleDetailPageComponent {
  protected readonly vehicle = signal<VehicleDetailResponse | null>(null);
  protected readonly upcomingMaintenance = signal<UpcomingMaintenanceResponse[]>([]);
  protected readonly completedMaintenance = signal<CompletedMaintenanceResponse[]>([]);
  protected readonly uncompletedRecalls = signal<RecallResponse[]>([]);
  protected readonly completedRecalls = signal<CompletedRecallResponse[]>([]);
  protected readonly activeSection = signal<ActiveSection>('maintenance');
  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly isMileageModalOpen = signal(false);
  protected readonly selectedUpcomingMaintenance = signal<UpcomingMaintenanceResponse | null>(null);
  protected readonly selectedCompletedMaintenance = signal<CompletedMaintenanceResponse | null>(
    null,
  );
  protected readonly selectedUncompletedRecall = signal<RecallResponse | null>(null);
  protected readonly selectedCompletedRecall = signal<CompletedRecallResponse | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly vinService = inject(VinService);
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly recallService = inject(RecallService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const vin = params.get('vin');
      if (vin) {
        this.loadPage(vin);
      }
    });
  }

  protected setActiveSection(section: ActiveSection): void {
    this.activeSection.set(section);
  }

  protected openMileageModal(): void {
    this.isMileageModalOpen.set(true);
  }

  protected closeMileageModal(): void {
    this.isMileageModalOpen.set(false);
  }

  protected onMileageUpdated(detail: VehicleDetailResponse): void {
    this.vehicle.set(detail);
  }

  protected openUpcomingMaintenance(item: UpcomingMaintenanceResponse): void {
    this.selectedUpcomingMaintenance.set(item);
    this.selectedCompletedMaintenance.set(null);
  }

  protected openCompletedMaintenance(item: CompletedMaintenanceResponse): void {
    this.selectedCompletedMaintenance.set(item);
    this.selectedUpcomingMaintenance.set(null);
  }

  protected closeMaintenanceModal(): void {
    this.selectedUpcomingMaintenance.set(null);
    this.selectedCompletedMaintenance.set(null);
  }

  protected openUncompletedRecall(item: RecallResponse): void {
    this.selectedUncompletedRecall.set(item);
    this.selectedCompletedRecall.set(null);
  }

  protected openCompletedRecall(item: CompletedRecallResponse): void {
    this.selectedCompletedRecall.set(item);
    this.selectedUncompletedRecall.set(null);
  }

  protected closeRecallModal(): void {
    this.selectedUncompletedRecall.set(null);
    this.selectedCompletedRecall.set(null);
  }

  protected onMaintenanceChanged(): void {
    const vin = this.vehicle()?.vin;
    if (vin) {
      this.loadMaintenanceLists(vin);
    }
  }

  protected onRecallChanged(): void {
    const vin = this.vehicle()?.vin;
    if (vin) {
      this.loadRecallLists(vin);
    }
  }

  private loadPage(vin: string): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.closeMaintenanceModal();
    this.closeRecallModal();

    this.vinService
      .getVehicleDetail(vin)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.vehicle.set(detail);
          this.isLoading.set(false);
          this.loadMaintenanceLists(vin);
          this.loadRecallLists(vin);
        },
        error: (err: HttpErrorResponse) => {
          this.vehicle.set(null);
          this.error.set(err.status === 404 ? 'Vehicle not found.' : 'Failed to load vehicle.');
          this.isLoading.set(false);
        },
      });
  }

  private loadMaintenanceLists(vin: string): void {
    forkJoin({
      upcoming: this.maintenanceService.getUpcoming(vin),
      completed: this.maintenanceService.getCompleted(vin),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ upcoming, completed }) => {
          this.upcomingMaintenance.set(upcoming);
          this.completedMaintenance.set(completed);
        },
        error: () => {
          this.upcomingMaintenance.set([]);
          this.completedMaintenance.set([]);
        },
      });
  }

  private loadRecallLists(vin: string): void {
    forkJoin({
      uncompleted: this.recallService.getUncompleted(vin),
      completed: this.recallService.getCompleted(vin),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ uncompleted, completed }) => {
          this.uncompletedRecalls.set(uncompleted);
          this.completedRecalls.set(completed);
        },
        error: () => {
          this.uncompletedRecalls.set([]);
          this.completedRecalls.set([]);
        },
      });
  }
}
