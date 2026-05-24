import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { VinService } from '../../core/vin/vin.service';

@Component({
  selector: 'app-delete-vehicle-modal',
  standalone: true,
  templateUrl: './delete-vehicle-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeleteVehicleModalComponent implements AfterViewInit {
  readonly vin = input.required<string>();
  readonly close = output<void>();
  readonly deleted = output<string>();

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<string | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly vinService = inject(VinService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected submit(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.serverError.set(null);
    this.isSubmitting.set(true);

    this.vinService
      .deleteVehicle(this.vin())
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.deleted.emit(this.vin());
          this.close.emit();
        },
        error: (error) => {
          const msg = error?.error?.message || 'An error occurred deleting the vehicle.';
          this.serverError.set(msg);
        },
      });
  }
}
