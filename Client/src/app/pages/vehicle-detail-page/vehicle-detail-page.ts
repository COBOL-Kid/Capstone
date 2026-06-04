import { DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { EMPTY, filter, map } from 'rxjs';

import { VehicleDetailResponse } from '../../core/vin/vin.models';
import { VehiclePageData } from '../../core/vin/vehicle-page.models';
import { VehiclePageDataService } from '../../core/vin/vehicle-page.data';
import { UserVehiclesStore } from '../../core/vin/user-vehicles.store';
import {
  CompletedMaintenanceResponse,
  LaborCostResponse,
  PartCostResponse,
  SelectedUpcomingMaintenance,
} from '../../core/maintenance/maintenance.models';
import { CompletedRecallResponse, RecallResponse } from '../../core/recall/recall.models';
import { UpdateMileageModalComponent } from '../../components/update-mileage-modal/update-mileage-modal';
import { MaintenanceDetailModalComponent } from '../../components/maintenance-detail-modal/maintenance-detail-modal';
import { MaintenanceCostsModalComponent } from '../../components/maintenance-costs-modal/maintenance-costs-modal';
import { RecallDetailModalComponent } from '../../components/recall-detail-modal/recall-detail-modal';
import { WarrantyModalComponent } from '../../components/warranty-modal/warranty-modal';
import { VehiclePhotoModalComponent } from '../../components/vehicle-photo-modal/vehicle-photo-modal';
import {
  formatWarrantyCoverageLabelForSpecs,
  formatWarrantyCoverageStatus,
} from '../../core/warranty/warranty-display';
type ActiveSection = 'maintenance' | 'recalls';

@Component({
  selector: 'app-vehicle-detail-page',
  standalone: true,
  imports: [
    DecimalPipe,
    RouterLink,
    UpdateMileageModalComponent,
    MaintenanceDetailModalComponent,
    MaintenanceCostsModalComponent,
    RecallDetailModalComponent,
    WarrantyModalComponent,
    VehiclePhotoModalComponent,
  ],
  templateUrl: './vehicle-detail-page.html',
  styleUrl: './vehicle-detail-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehicleDetailPageComponent {
  protected readonly pageData = signal<VehiclePageData | null>(null);
  protected readonly activeSection = signal<ActiveSection>('maintenance');
  protected readonly isMileageModalOpen = signal(false);
  protected readonly isPhotoModalOpen = signal(false);
  protected readonly isMaintenanceCostsModalOpen = signal(false);
  protected readonly isWarrantyModalOpen = signal(false);
  protected readonly selectedUpcomingMaintenance = signal<SelectedUpcomingMaintenance | null>(null);
  protected readonly selectedCompletedMaintenance = signal<CompletedMaintenanceResponse | null>(
    null,
  );
  protected readonly selectedUncompletedRecall = signal<RecallResponse | null>(null);
  protected readonly selectedCompletedRecall = signal<CompletedRecallResponse | null>(null);
  protected readonly showInspectItems = signal(true);

  private readonly route = inject(ActivatedRoute);
  private readonly vehiclePageData = inject(VehiclePageDataService);
  private readonly vehiclesStore = inject(UserVehiclesStore);
  private readonly destroyRef = inject(DestroyRef);

  private readonly routeVin = toSignal(
    this.route.paramMap.pipe(
      map((params) => params.get('vin')),
      filter((vin): vin is string => vin !== null),
    ),
  );

  private readonly pageResource = rxResource({
    params: () => this.routeVin(),
    stream: ({ params: vin }) => {
      if (!vin) {
        return EMPTY;
      }
      return this.vehiclePageData.loadVehiclePage(vin);
    },
  });

  protected readonly vehicle = computed(() => this.pageData()?.detail ?? null);
  protected readonly heroSelectedImageUrl = computed(() => this.vehicle()?.selectedImageUrl ?? '');
  protected readonly upcomingIntervals = computed(() => this.pageData()?.upcomingIntervals ?? []);
  protected readonly completedMaintenance = computed(
    () => this.pageData()?.completedMaintenance ?? [],
  );
  protected readonly uncompletedRecalls = computed(() => this.pageData()?.uncompletedRecalls ?? []);
  protected readonly completedRecalls = computed(() => this.pageData()?.completedRecalls ?? []);
  protected readonly upcomingItemCount = computed(() =>
    this.upcomingIntervals().reduce((total, interval) => total + interval.items.length, 0),
  );
  protected readonly miscMaintenanceCosts = computed(
    () => this.pageData()?.miscMaintenanceCosts ?? [],
  );
  protected readonly hasMaintenanceCostEstimates = computed(
    () => this.miscMaintenanceCosts().length > 0,
  );
  protected readonly vehicleWarranty = computed(() => this.pageData()?.vehicleWarranty ?? null);
  protected readonly hasWarrantyInformation = computed(() => this.vehicleWarranty() !== null);
  protected readonly hasOwnersManual = computed(() => {
    const url = this.vehicle()?.ownersManual;
    return url != null && url !== '';
  });
  protected readonly informationUnavailableMessage =
    'information not yet available for this vehicle';
  protected readonly warrantyCoveragesForDisplay = computed(() => {
    const warranty = this.vehicleWarranty();
    if (!warranty) {
      return [];
    }
    return warranty.coverages.filter((coverage) => coverage.estimatedExpirationDate != null);
  });
  protected readonly isLoading = computed(() => {
    const status = this.pageResource.status();
    return status === 'loading' || status === 'reloading';
  });
  protected readonly error = computed(() => {
    const err = this.pageResource.error();
    if (!err) {
      return null;
    }
    if (err instanceof HttpErrorResponse && err.status === 404) {
      return 'Vehicle not found.';
    }
    return 'Failed to load vehicle.';
  });

  constructor() {
    effect(() => {
      const vin = this.routeVin();
      if (vin) {
        this.beginPageLoad();
      }
    });

    effect(() => {
      const data = this.pageResource.value();
      if (!data) {
        return;
      }

      const current = untracked(() => this.pageData());
      const vin = data.detail.vin;
      const preserveSelectedImageUrl =
        current?.detail.vin === vin &&
        current.detail.selectedImageUrl !== data.detail.selectedImageUrl;

      if (preserveSelectedImageUrl) {
        this.pageData.set({
          ...data,
          detail: { ...data.detail, selectedImageUrl: current.detail.selectedImageUrl },
        });
        return;
      }

      if (
        current?.detail.vin === data.detail.vin &&
        current.detail.selectedImageUrl === data.detail.selectedImageUrl
      ) {
        return;
      }

      this.pageData.set(data);
    });

    effect(() => {
      if (this.pageResource.error()) {
        this.pageData.set(null);
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

  protected openPhotoModal(): void {
    this.isPhotoModalOpen.set(true);
  }

  protected closePhotoModal(): void {
    this.isPhotoModalOpen.set(false);
  }

  protected openWarrantyModal(): void {
    if (!this.hasWarrantyInformation()) {
      return;
    }
    this.isWarrantyModalOpen.set(true);
  }

  protected closeWarrantyModal(): void {
    this.isWarrantyModalOpen.set(false);
  }

  protected openMaintenanceCostsModal(): void {
    if (!this.hasMaintenanceCostEstimates()) {
      return;
    }
    this.isMaintenanceCostsModalOpen.set(true);
  }

  protected closeMaintenanceCostsModal(): void {
    this.isMaintenanceCostsModalOpen.set(false);
  }

  protected openOwnersManual(): void {
    const url = this.vehicle()?.ownersManual;
    if (!url) {
      return;
    }
    const tab = window.open(url, '_blank');
    if (tab) {
      tab.opener = null;
    }
  }

  protected onMileageUpdated(_detail: VehicleDetailResponse): void {
    this.closeMileageModal();
    this.refreshPageData();
  }

  protected onPhotoUpdated(selectedImageUrl: string): void {
    const data = this.pageData();
    if (!data) {
      return;
    }

    const vin = data.detail.vin;
    this.pageData.set({
      ...data,
      detail: {
        ...data.detail,
        selectedImageUrl,
      },
    });
    this.vehiclesStore.updateSelectedPhoto(vin, selectedImageUrl);
    this.closePhotoModal();
    this.refreshPageData();
  }

  protected openUpcomingMaintenanceFromItem(
    mileageDue: number,
    item: { maintMileageId: number; maintDesc: string },
    event: Event,
  ): void {
    event.preventDefault();
    event.stopPropagation();
    this.selectedUpcomingMaintenance.set({
      maintMileageId: item.maintMileageId,
      mileageDue,
      maintDesc: item.maintDesc,
    });
    this.selectedCompletedMaintenance.set(null);
  }

  protected formatCurrency(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
    }).format(amount);
  }

  protected onShowInspectItemsChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.showInspectItems.set(target.checked);
  }

  protected formatLaborCost(labor: LaborCostResponse | null): string {
    if (!labor) {
      return '';
    }
    return `${this.formatCurrency(labor.totalCost, labor.currency)} (${labor.timeRequiredHours}h @ ${this.formatCurrency(labor.hourlyRate, labor.currency)}/hr)`;
  }

  protected formatPartsCost(parts: PartCostResponse[]): string {
    const total = parts.reduce((sum, part) => sum + part.totalCost, 0);
    const currency = parts[0]?.currency ?? 'USD';
    return `Parts ${this.formatCurrency(total, currency)}`;
  }

  protected formatWarrantyCoverageLabelForSpecs = formatWarrantyCoverageLabelForSpecs;
  protected formatWarrantyCoverageStatus = formatWarrantyCoverageStatus;

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
    this.refreshPageData();
  }

  protected onRecallChanged(): void {
    this.refreshPageData();
  }

  private beginPageLoad(): void {
    this.closeMaintenanceModal();
    this.closeRecallModal();
    this.closeMaintenanceCostsModal();
    this.closeWarrantyModal();
    this.closePhotoModal();
  }

  /** Refetch dashboard after mutations; rxResource.reload() does not reliably sync pageData. */
  private refreshPageData(): void {
    const vin = this.routeVin();
    if (!vin) {
      return;
    }

    const preservedSelectedImageUrl =
      this.pageData()?.detail.vin === vin ? this.pageData()?.detail.selectedImageUrl : undefined;

    this.vehiclePageData
      .loadVehiclePage(vin)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          if (
            preservedSelectedImageUrl &&
            data.detail.vin === vin &&
            data.detail.selectedImageUrl !== preservedSelectedImageUrl
          ) {
            this.pageData.set({
              ...data,
              detail: { ...data.detail, selectedImageUrl: preservedSelectedImageUrl },
            });
            return;
          }

          this.pageData.set(data);
        },
      });
  }
}
