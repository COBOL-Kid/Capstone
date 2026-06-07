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

import { AuthService } from '../../core/auth/auth.service';
import { AuthErrorMessage } from '../../core/auth/auth.models';
import { EmailVerificationStepComponent } from '../email-verification-step/email-verification-step';

type ChangeEmailStep = 'details' | 'verify';

@Component({
  selector: 'app-change-email-modal',
  standalone: true,
  imports: [ReactiveFormsModule, EmailVerificationStepComponent],
  templateUrl: './change-email-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangeEmailModalComponent implements AfterViewInit {
  readonly currentEmail = input.required<string>();
  readonly startOnVerifyStep = input(false);
  readonly close = output<void>();
  readonly changed = output<void>();

  protected readonly step = signal<ChangeEmailStep>('details');
  protected readonly isSubmitting = signal(false);
  protected readonly isVerifying = signal(false);
  protected readonly isResending = signal(false);
  protected readonly serverError = signal<AuthErrorMessage | null>(null);
  protected readonly verificationError = signal<AuthErrorMessage | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    newEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
  });
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    if (this.startOnVerifyStep()) {
      this.step.set('verify');
    }
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected submitDetails(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.serverError.set(null);

    const newEmail = this.form.controls.newEmail.value.trim();
    this.form.patchValue({ newEmail }, { emitEvent: false });

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.authService
      .initiateAccountChange({ changeType: 'EMAIL', newEmail })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.verificationError.set(null);
          this.step.set('verify');
        },
        error: (error: AuthErrorMessage) => this.serverError.set(error),
      });
  }

  protected verifyCode(code: string): void {
    if (this.isVerifying()) {
      return;
    }

    this.verificationError.set(null);
    this.isVerifying.set(true);
    this.authService
      .verifyAccountChange(code)
      .pipe(
        finalize(() => this.isVerifying.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.changed.emit();
          this.close.emit();
        },
        error: (error: AuthErrorMessage) => this.verificationError.set(error),
      });
  }

  protected resendCode(): void {
    if (this.isResending()) {
      return;
    }

    this.verificationError.set(null);
    this.isResending.set(true);
    this.authService
      .resendAccountChangeCode()
      .pipe(
        finalize(() => this.isResending.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (error: AuthErrorMessage) => this.verificationError.set(error),
      });
  }
}
