import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  HostListener,
  inject,
  input,
  OnInit,
  output,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AddVinRequest, VinErrorMessage } from '../../core/vin/vin.models';

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
export class AddVehicleModalComponent implements OnInit, AfterViewInit {
  readonly close = output<void>();
  readonly submitRequest = output<AddVinRequest>();
  readonly serverErrorClear = output<void>();
  readonly isSubmitting = input(false);
  readonly serverError = input<VinErrorMessage | null>(null);

  protected readonly vinValidationMessage = vinValidationMessage;

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);
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

  constructor() {
    effect(() => {
      if (this.isSubmitting()) {
        this.form.disable({ emitEvent: false });
        return;
      }
      this.form.enable({ emitEvent: false });
    });
  }

  ngOnInit(): void {
    this.form.controls.vin.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.serverError()) {
        this.serverErrorClear.emit();
      }
    });
  }

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

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitRequest.emit({
      vin: value.vin.trim().toUpperCase(),
      currentMileage: value.currentMileage,
    });
  }
}
