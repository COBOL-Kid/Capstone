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
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, EMPTY, finalize } from 'rxjs';

import {
  AddVinRequest,
  AddVinTrimSelectionRequiredResponse,
  VinErrorMessage,
} from '../../core/vin/vin.models';
import { VinService } from '../../core/vin/vin.service';

const vinPattern = /^[A-HJ-NPR-Z0-9]{17}$/i;
const vinValidationMessage = 'VIN must be 17 characters and cannot contain I, O, or Q';

type AddVehicleStep = 'form' | 'limitedDataWarning' | 'trimSelection';

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
  readonly trimSelectionContext = input<AddVinTrimSelectionRequiredResponse | null>(null);

  protected readonly vinValidationMessage = vinValidationMessage;
  protected readonly step = signal<AddVehicleStep>('form');
  protected readonly trimOptions = signal<string[]>([]);
  protected readonly selectedTrim = signal('');
  protected readonly trimLoadError = signal<string | null>(null);
  protected readonly isLoadingTrims = signal(false);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly vinService = inject(VinService);
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
      if (this.step() === 'form') {
        this.form.enable({ emitEvent: false });
      } else {
        this.form.disable({ emitEvent: false });
      }
    });

    effect(() => {
      const context = this.trimSelectionContext();
      if (context) {
        this.step.set('limitedDataWarning');
        this.trimOptions.set([]);
        this.selectedTrim.set('');
        this.trimLoadError.set(null);
      }
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
    if (this.isSubmitting() || this.isLoadingTrims()) {
      return;
    }
    this.resetFlow();
    this.close.emit();
  }

  protected submit(): void {
    if (this.isSubmitting() || this.step() !== 'form') {
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

  protected continueDespiteLimitedData(): void {
    const context = this.trimSelectionContext();
    if (!context) {
      return;
    }
    this.step.set('trimSelection');
    this.trimLoadError.set(null);
    this.loadTrimOptions(context);
  }

  protected onTrimFieldInteract(): void {
    const context = this.trimSelectionContext();
    if (!context || this.trimOptions().length > 0 || this.isLoadingTrims()) {
      return;
    }
    this.loadTrimOptions(context);
  }

  protected confirmTrimSelection(): void {
    if (this.isSubmitting()) {
      return;
    }
    const trim = this.selectedTrim().trim();
    if (!trim) {
      this.trimLoadError.set('Select a trim to continue.');
      return;
    }
    const value = this.form.getRawValue();
    this.submitRequest.emit({
      vin: value.vin.trim().toUpperCase(),
      currentMileage: value.currentMileage,
      selectedTrim: trim,
    });
  }

  protected onTrimChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedTrim.set(select.value);
    if (this.trimLoadError()) {
      this.trimLoadError.set(null);
    }
  }

  private loadTrimOptions(context: AddVinTrimSelectionRequiredResponse): void {
    this.isLoadingTrims.set(true);
    this.trimLoadError.set(null);
    this.vinService
      .getTrimOptions(context.year, context.make, context.model)
      .pipe(
        finalize(() => this.isLoadingTrims.set(false)),
        catchError(() => {
          this.trimLoadError.set('Unable to load trim options. Please try again.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((trims) => {
        this.trimOptions.set(trims);
        if (trims.length === 0) {
          this.trimLoadError.set('No trim options are available for this vehicle.');
        }
      });
  }

  private resetFlow(): void {
    this.step.set('form');
    this.trimOptions.set([]);
    this.selectedTrim.set('');
    this.trimLoadError.set(null);
  }
}
