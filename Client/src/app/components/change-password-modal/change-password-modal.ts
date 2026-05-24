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

import { AuthService } from '../../core/auth/auth.service';
import { AuthErrorMessage } from '../../core/auth/auth.models';

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;

@Component({
  selector: 'app-change-password-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './change-password-modal.html',
  host: {
    class: 'hc-modal-host',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordModalComponent implements AfterViewInit {
  readonly close = output<void>();
  readonly changed = output<void>();

  protected readonly isSubmitting = signal(false);
  protected readonly serverError = signal<AuthErrorMessage | null>(null);

  private readonly dialog = viewChild<ElementRef<HTMLElement>>('dialog');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72),
        Validators.pattern(passwordPattern),
      ],
    ],
    confirmPassword: ['', [Validators.required]],
  });
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    this.dialog()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected handleEscape(): void {
    this.close.emit();
  }

  protected passwordConfirmationMatches(): boolean {
    const value = this.form.getRawValue();

    return value.newPassword === value.confirmPassword;
  }

  protected submit(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.serverError.set(null);

    if (this.form.invalid || !this.passwordConfirmationMatches()) {
      this.form.markAllAsTouched();

      return;
    }

    const value = this.form.getRawValue();
    this.isSubmitting.set(true);

    this.authService
      .changePassword({ currentPassword: value.currentPassword, newPassword: value.newPassword })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.changed.emit();
          this.close.emit();
        },
        error: (error: AuthErrorMessage) => this.serverError.set(error),
      });
  }
}
