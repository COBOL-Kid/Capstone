import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { VinService } from '../../core/vin/vin.service';
import { AddVinResponse, VinErrorMessage } from '../../core/vin/vin.models';

const vinPattern = /^[A-HJ-NPR-Z0-9]{17}$/i;
const vinValidationMessage = 'VIN must be 17 characters and cannot contain I, O, or Q';

@Component({
  selector: 'app-add-vehicle-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './add-vehicle-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddVehicleModalComponent implements AfterViewInit {
  readonly close = output<void>();
  readonly added = output<AddVinResponse>();
  readonly submittingChange = output<boolean>();

  protected readonly vinValidationMessage = vinValidationMessage;

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<VinErrorMessage | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    vin: [
      '',
      [
        Validators.required,
        Validators.minLength(17),
        Validators.maxLength(17),
        Validators.pattern(vinPattern),
      ],
    ],
    currentMileage: [0, [Validators.required, Validators.min(0)]],
  });
  private readonly vinService = inject(VinService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    if (this.isSubmitting()) {
      return;
    }
    this.close.emit();
  }

  protected submit(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.serverError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.submittingChange.emit(true);

    const value = this.form.getRawValue();
    this.vinService
      .addVehicle({
        vin: value.vin.trim().toUpperCase(),
        currentMileage: value.currentMileage,
      })
      .pipe(
        finalize(() => {
          this.isSubmitting.set(false);
          this.submittingChange.emit(false);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.added.emit(response);
          this.close.emit();
        },
        error: (error: VinErrorMessage) => {
          this.serverError.set(error);
        },
      });
  }
}
