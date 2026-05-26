import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  input,
  OnInit,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MaintenanceService } from '../../core/maintenance/maintenance.service';
import { MaintenanceCostResponse } from '../../core/maintenance/maintenance.models';

@Component({
  selector: 'app-maintenance-costs-modal',
  standalone: true,
  templateUrl: './maintenance-costs-modal.html',
  styleUrl: './maintenance-costs-modal.css',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MaintenanceCostsModalComponent implements OnInit, AfterViewInit {
  readonly vin = input.required<string>();
  readonly close = output<void>();

  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly allCosts = signal<MaintenanceCostResponse[]>([]);
  protected readonly searchQuery = signal('');

  protected readonly filteredCosts = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const costs = this.allCosts();
    if (!query) {
      return costs;
    }
    return costs.filter(
      (cost) =>
        cost.maintTitle.toLowerCase().includes(query) ||
        (cost.maintDesc?.toLowerCase().includes(query) ?? false),
    );
  });

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadCosts();
  }

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
  }

  protected formatCostRange(low: number | null, high: number | null, avg: number | null): string {
    if (low != null && high != null) {
      return `$${low} – $${high}`;
    }
    if (avg != null) {
      return `$${avg}`;
    }
    return '—';
  }

  private loadCosts(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.maintenanceService
      .getMaintenanceCosts(this.vin())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (costs) => {
          this.allCosts.set(costs);
          this.isLoading.set(false);
        },
        error: () => {
          this.allCosts.set([]);
          this.error.set('Unable to load maintenance costs. Please try again.');
          this.isLoading.set(false);
        },
      });
  }
}
