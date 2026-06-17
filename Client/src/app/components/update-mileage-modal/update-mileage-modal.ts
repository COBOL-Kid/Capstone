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

import { VinService } from '../../core/vin/vin.service';
import { VehicleDetailResponse } from '../../core/vin/vin.models';

@Component({
  selector: 'app-update-mileage-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './update-mileage-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UpdateMileageModalComponent implements AfterViewInit {
  readonly vin = input.required<string>();
  readonly currentMileage = input.required<number>();
  readonly close = output<void>();
  readonly updated = output<VehicleDetailResponse>();

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<string | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    currentMileage: [0, [Validators.required, Validators.min(0)]],
  });
  private readonly vinService = inject(VinService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.form.patchValue({ currentMileage: this.currentMileage() });
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.requestClose();
  }

  protected requestClose(): void {
    if (this.isSubmitting()) {
      return;
    }
    this.close.emit();
  }

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.serverError.set(null);
    this.isSubmitting.set(true);

    this.vinService
      .updateMileage(this.vin(), { currentMileage: this.form.controls.currentMileage.value })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (detail) => {
          this.updated.emit(detail);
          this.close.emit();
        },
        error: () => {
          this.serverError.set('Unable to update mileage. Please try again.');
        },
      });
  }
}
