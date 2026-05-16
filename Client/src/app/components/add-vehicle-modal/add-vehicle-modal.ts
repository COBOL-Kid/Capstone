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
import { AddVinResponse } from '../../core/vin/vin.models';

@Component({
  selector: 'app-add-vehicle-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './add-vehicle-modal.html',
  styleUrl: '../auth-modal/auth-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddVehicleModalComponent implements AfterViewInit {
  readonly close = output<void>();
  readonly added = output<AddVinResponse>();

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<string | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    vin: ['', [Validators.required, Validators.pattern(/^[A-HJ-NPR-Z0-9]{17}$/i)]],
    currentMileage: [0, [Validators.required, Validators.min(0)]],
  });
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

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const value = this.form.getRawValue();
    this.vinService
      .addVehicle({
        vin: value.vin.trim().toUpperCase(),
        currentMileage: value.currentMileage,
      })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.added.emit(response);
          this.close.emit();
        },
        error: (error) => {
          const msg = error?.error?.message || 'An error occurred adding the vehicle.';
          this.serverError.set(msg);
        },
      });
  }
}
