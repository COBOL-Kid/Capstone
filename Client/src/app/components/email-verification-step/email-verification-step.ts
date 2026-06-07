import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthErrorMessage } from '../../core/auth/auth.models';

@Component({
  selector: 'app-email-verification-step',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './email-verification-step.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailVerificationStepComponent {
  readonly email = input('');
  readonly description = input(
    'We sent a 6-digit code to your email. Enter it below to complete this change.',
  );
  readonly isSubmitting = input(false);
  readonly isResending = input(false);
  readonly error = input<AuthErrorMessage | null>(null);
  readonly submitLabel = input('Verify');
  readonly resendLabel = input('Resend code');
  readonly showCancel = input(false);

  readonly verify = output<string>();
  readonly resend = output<void>();
  readonly cancel = output<void>();

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  protected submit(): void {
    if (this.isSubmitting() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.verify.emit(this.form.controls.code.value);
  }

  protected onResend(): void {
    if (this.isResending()) {
      return;
    }

    this.resend.emit();
  }
}
