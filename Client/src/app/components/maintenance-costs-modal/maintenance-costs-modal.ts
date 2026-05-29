import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  HostListener,
  input,
  OnInit,
  output,
  signal,
  viewChild,
} from '@angular/core';

import { MiscMaintenanceCostResponse } from '../../core/maintenance/maintenance.models';

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
  readonly costs = input.required<MiscMaintenanceCostResponse[]>();
  readonly close = output<void>();

  protected readonly searchQuery = signal('');

  protected readonly filteredCosts = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const allCosts = this.costs();
    if (!query) {
      return allCosts;
    }
    return allCosts.filter(
      (cost) =>
        cost.maintTitle.toLowerCase().includes(query) ||
        (cost.maintDesc?.toLowerCase().includes(query) ?? false),
    );
  });

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');

  ngOnInit(): void {}

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
}
