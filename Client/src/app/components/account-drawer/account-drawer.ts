import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  HostListener,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { AccountDetails, AuthErrorMessage, AuthModalMode } from '../../core/auth/auth.models';
import { AuthModalComponent } from '../auth-modal/auth-modal';

type AccountDrawerStatus = 'signed-out' | 'loading' | 'signed-in' | 'unauthorized' | 'error';
const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;

@Component({
  selector: 'app-account-drawer',
  imports: [AuthModalComponent, ReactiveFormsModule],
  templateUrl: './account-drawer.html',
  styleUrl: './account-drawer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountDrawerComponent {
  readonly isOpen = signal(false);
  readonly isClosing = signal(false);
  readonly status = signal<AccountDrawerStatus>('signed-out');
  readonly account = signal<AccountDetails | null>(null);
  readonly authModalMode = signal<AuthModalMode | null>(null);
  readonly isEditingProfile = signal(false);
  readonly isSubmittingProfile = signal(false);
  readonly isSubmittingPassword = signal(false);
  readonly profileSuccessMessage = signal<string | null>(null);
  readonly passwordSuccessMessage = signal<string | null>(null);
  readonly profileServerError = signal<AuthErrorMessage | null>(null);
  readonly passwordServerError = signal<AuthErrorMessage | null>(null);
  private readonly closeDelayMs = 240;
  private readonly fb = inject(NonNullableFormBuilder);
  readonly profileForm = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    userSms: ['', [Validators.maxLength(20), Validators.pattern(/^$|^(?=.*\d)[+0-9() .-]+$/)]],
  });
  readonly passwordForm = this.fb.group({
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
  private closeTimer: ReturnType<typeof window.setTimeout> | null = null;

  open(): void {
    this.clearCloseTimer();
    this.isClosing.set(false);
    this.isOpen.set(true);
    this.loadAccountDetails();
  }

  close(): void {
    if (!this.isOpen() || this.isClosing()) {
      return;
    }

    this.resetPasswordForm();
    this.isClosing.set(true);
    this.closeTimer = window.setTimeout(() => {
      this.isOpen.set(false);
      this.isClosing.set(false);
      this.closeTimer = null;
    }, this.closeDelayMs);
  }

  startProfileEdit(): void {
    const account = this.account();

    if (!account) {
      return;
    }

    this.resetProfileForm(account);
    this.profileServerError.set(null);
    this.profileSuccessMessage.set(null);
    this.isEditingProfile.set(true);
  }

  cancelProfileEdit(): void {
    const account = this.account();

    if (account) {
      this.resetProfileForm(account);
    }

    this.profileServerError.set(null);
    this.isEditingProfile.set(false);
  }

  submitProfile(): void {
    const account = this.account();

    if (!account || this.isSubmittingProfile()) {
      return;
    }

    this.profileServerError.set(null);
    this.profileSuccessMessage.set(null);

    const rawValue = this.profileForm.getRawValue();
    const requestedEmail = rawValue.email.trim();
    const requestedSms = rawValue.userSms.trim();
    this.profileForm.patchValue(
      { email: requestedEmail, userSms: requestedSms },
      { emitEvent: false },
    );

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();

      return;
    }

    const emailChanged = requestedEmail.toLowerCase() !== account.email.toLowerCase();
    this.isSubmittingProfile.set(true);

    this.authService
      .updateCurrentAccount({
        firstName: account.firstName,
        lastName: account.lastName,
        email: requestedEmail,
        userSms: requestedSms || null,
      })
      .pipe(
        switchMap((updatedAccount) => {
          this.account.set(updatedAccount);
          this.resetProfileForm(updatedAccount);

          return emailChanged
            ? this.authService.refresh().pipe(catchError(() => of(null)))
            : of(null);
        }),
        finalize(() => this.isSubmittingProfile.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.profileSuccessMessage.set('Account details updated.');
          this.isEditingProfile.set(false);
        },
        error: (error: AuthErrorMessage) => this.profileServerError.set(error),
      });
  }

  submitPassword(): void {
    if (this.isSubmittingPassword()) {
      return;
    }

    this.passwordServerError.set(null);
    this.passwordSuccessMessage.set(null);

    if (this.passwordForm.invalid || !this.passwordConfirmationMatches()) {
      this.passwordForm.markAllAsTouched();

      return;
    }
    const value = this.passwordForm.getRawValue();
    this.isSubmittingPassword.set(true);

    this.authService
      .changePassword({ currentPassword: value.currentPassword, newPassword: value.newPassword })
      .pipe(
        finalize(() => this.isSubmittingPassword.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.passwordForm.reset();
          this.passwordSuccessMessage.set('Password updated.');
        },
        error: (error: AuthErrorMessage) => this.passwordServerError.set(error),
      });
  }

  passwordConfirmationMatches(): boolean {
    const value = this.passwordForm.getRawValue();

    return value.newPassword === value.confirmPassword;
  }

  openAuthModal(mode: AuthModalMode): void {
    this.authModalMode.set(mode);
  }

  setAuthModalMode(mode: AuthModalMode): void {
    this.authModalMode.set(mode);
  }

  closeAuthModal(): void {
    this.authModalMode.set(null);

    if (this.isOpen()) {
      this.loadAccountDetails();
    }
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    if (this.isOpen()) {
      this.close();
    }
  }

  private loadAccountDetails(): void {
    this.account.set(null);

    if (!this.authService.isSignedIn()) {
      this.resetPasswordForm();
      this.status.set('signed-out');
      return;
    }

    this.status.set('loading');

    this.authService.getCurrentAccount().subscribe({
      next: (account) => {
        this.account.set(account);
        this.resetProfileForm(account);
        this.passwordForm.reset();
        this.isEditingProfile.set(false);
        this.profileServerError.set(null);
        this.passwordServerError.set(null);
        this.profileSuccessMessage.set(null);
        this.passwordSuccessMessage.set(null);
        this.status.set('signed-in');
      },
      error: (error: unknown) => {
        this.account.set(null);
        this.resetPasswordForm();

        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.authService.clearSession();
          this.status.set('unauthorized');
          return;
        }

        this.status.set('error');
      },
    });
  }

  private clearCloseTimer(): void {
    if (this.closeTimer === null) {
      return;
    }

    window.clearTimeout(this.closeTimer);
    this.closeTimer = null;
  }

  private resetProfileForm(account: AccountDetails): void {
    this.profileForm.reset({
      email: account.email,
      userSms: account.userSms ?? '',
    });
  }

  private resetPasswordForm(): void {
    this.passwordForm.reset();
    this.passwordServerError.set(null);
    this.passwordSuccessMessage.set(null);
  }
}
