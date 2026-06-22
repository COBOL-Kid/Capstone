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
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { finalize } from 'rxjs';

import { isSafeHttpUrl } from '../../core/http/safe-url';
import {
  VehicleListingResponse,
  VehicleListingsResponse,
} from '../../core/listings/listings.models';
import { VinService } from '../../core/vin/vin.service';

@Component({
  selector: 'app-vehicle-listings-modal',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './vehicle-listings-modal.html',
  styleUrl: './vehicle-listings-modal.css',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehicleListingsModalComponent implements OnInit, AfterViewInit {
  readonly vin = input.required<string>();
  readonly close = output<void>();

  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly response = signal<VehicleListingsResponse | null>(null);

  protected readonly listings = computed(() => this.response()?.listings ?? []);
  protected readonly subtitle = computed(() => {
    const data = this.response();
    if (!data) {
      return '';
    }
    return `${data.year} ${data.make} ${data.model}`;
  });
  protected readonly totalCount = computed(() => this.response()?.total ?? null);
  protected readonly pricingSummary = computed(() => this.response()?.pricingSummary ?? null);
  protected readonly hasPricingSummary = computed(() => {
    const summary = this.pricingSummary();
    return summary != null && summary.pricedListingCount > 0;
  });

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly destroyRef = inject(DestroyRef);
  private readonly vinService = inject(VinService);

  ngOnInit(): void {
    this.loadListings();
  }

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.requestClose();
  }

  protected requestClose(): void {
    if (this.isLoading()) {
      return;
    }
    this.close.emit();
  }

  protected formatPrice(price: number | null): string {
    if (price == null) {
      return '—';
    }
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 0,
    }).format(price);
  }

  protected formatLocation(listing: VehicleListingResponse): string {
    const parts = [listing.city, listing.state].filter((part) => part != null && part !== '');
    return parts.join(', ');
  }

  protected formatHistorySummary(listing: VehicleListingResponse): string | null {
    const history = listing.history;
    if (!history) {
      return null;
    }

    const parts: string[] = [];
    if (history.accidents === true) {
      const count = history.accidentCount;
      parts.push(
        count != null && count > 0
          ? `${count} accident${count === 1 ? '' : 's'}`
          : 'Accidents reported',
      );
    } else if (history.accidents === false) {
      parts.push('No accidents reported');
    }

    if (history.oneOwner === true) {
      parts.push('One owner');
    } else if (history.ownerCount != null && history.ownerCount > 0) {
      parts.push(`${history.ownerCount} owner${history.ownerCount === 1 ? '' : 's'}`);
    }

    if (history.usageType) {
      parts.push(history.usageType);
    }

    return parts.length > 0 ? parts.join(' · ') : null;
  }

  protected hasSafeUrl(url: string | null): boolean {
    return isSafeHttpUrl(url);
  }

  protected openExternalUrl(url: string): void {
    if (!isSafeHttpUrl(url)) {
      return;
    }
    const tab = window.open(url, '_blank');
    if (tab) {
      tab.opener = null;
    }
  }

  private loadListings(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.vinService
      .getVehicleListings(this.vin())
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (data) => this.response.set(data),
        error: (error) => {
          this.response.set(null);
          this.errorMessage.set(this.toErrorMessage(error));
        },
      });
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 429 && typeof error.error === 'string' && error.error.trim()) {
        return error.error.trim();
      }
    }
    return 'Unable to load vehicle listings right now. Please try again later.';
  }
}
