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

import { RecallService } from '../../core/recall/recall.service';
import { CompletedRecallResponse, RecallResponse } from '../../core/recall/recall.models';

@Component({
  selector: 'app-recall-detail-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './recall-detail-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecallDetailModalComponent implements AfterViewInit {
  readonly vin = input.required<string>();
  readonly isCompleted = input.required<boolean>();
  readonly uncompletedItem = input<RecallResponse | null>(null);
  readonly completedItem = input<CompletedRecallResponse | null>(null);

  readonly close = output<void>();
  readonly changed = output<void>();

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly showCompleteForm = signal(false);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly completeForm = this.fb.group({
    completedDate: [todayIso(), [Validators.required]],
    repairShop: [''],
    cost: [null as number | null],
    notes: [''],
  });
  private readonly recallService = inject(RecallService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected openCompleteForm(): void {
    this.showCompleteForm.set(true);
    this.completeForm.patchValue({ completedDate: todayIso() });
  }

  protected markComplete(): void {
    const recall = this.uncompletedItem();
    if (!recall || this.completeForm.invalid || this.isSubmitting()) {
      this.completeForm.markAllAsTouched();
      return;
    }

    this.serverError.set(null);
    this.isSubmitting.set(true);

    const { completedDate, repairShop, cost, notes } = this.completeForm.getRawValue();

    this.recallService
      .completeRecall({
        vin: this.vin(),
        recallId: recall.recallId,
        completedDate,
        repairShop: repairShop || undefined,
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
        error: () => this.serverError.set('Unable to mark recall complete.'),
      });
  }

  protected markIncomplete(): void {
    const completed = this.completedItem();
    if (!completed || this.isSubmitting()) {
      return;
    }

    this.serverError.set(null);
    this.isSubmitting.set(true);

    this.recallService
      .uncompleteRecall(completed.completedRecallId)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.changed.emit();
          this.close.emit();
        },
        error: () => this.serverError.set('Unable to mark recall incomplete.'),
      });
  }
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}
