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
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { MaintenanceService } from '../../core/maintenance/maintenance.service';
import {
  CompletedMaintenanceResponse,
  UpcomingMaintenanceResponse,
} from '../../core/maintenance/maintenance.models';

@Component({
  selector: 'app-maintenance-detail-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './maintenance-detail-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MaintenanceDetailModalComponent implements AfterViewInit {
  readonly vin = input.required<string>();
  readonly currentMileage = input.required<number>();
  readonly isCompleted = input.required<boolean>();
  readonly upcomingItem = input<UpcomingMaintenanceResponse | null>(null);
  readonly completedItem = input<CompletedMaintenanceResponse | null>(null);

  readonly close = output<void>();
  readonly changed = output<void>();

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly showCompleteForm = signal(false);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly completeForm = this.fb.group({
    completedDate: [todayIso(), [Validators.required]],
    mileageCompleted: [0, [Validators.required, Validators.min(0)]],
    cost: [null as number | null],
    notes: [''],
  });
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.completeForm.patchValue({ mileageCompleted: this.currentMileage() });
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected openCompleteForm(): void {
    this.showCompleteForm.set(true);
    this.completeForm.patchValue({
      completedDate: todayIso(),
      mileageCompleted: this.currentMileage(),
    });
  }

  protected markComplete(): void {
    const upcoming = this.upcomingItem();
    if (!upcoming || this.completeForm.invalid || this.isSubmitting()) {
      this.completeForm.markAllAsTouched();
      return;
    }

    this.serverError.set(null);
    this.isSubmitting.set(true);

    const { completedDate, mileageCompleted, cost, notes } = this.completeForm.getRawValue();

    this.maintenanceService
      .completeMaintenance({
        vin: this.vin(),
        maintMileageId: upcoming.maintMileageId,
        completedDate,
        mileageCompleted,
        cost: cost ?? undefined,
        notes: notes || undefined,
      })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.changed.emit();
          this.close.emit();
        },
        error: () => this.serverError.set('Unable to mark maintenance complete.'),
      });
  }

  protected markIncomplete(): void {
    const completed = this.completedItem();
    if (!completed || this.isSubmitting()) {
      return;
    }

    this.serverError.set(null);
    this.isSubmitting.set(true);

    this.maintenanceService
      .uncompleteMaintenance(completed.completedMaintenanceId)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.changed.emit();
          this.close.emit();
        },
        error: () => this.serverError.set('Unable to mark maintenance incomplete.'),
      });
  }
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}
