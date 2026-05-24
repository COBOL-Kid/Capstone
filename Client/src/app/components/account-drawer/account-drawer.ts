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
import { Router } from '@angular/router';
import { catchError, finalize, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { AccountDetails, AuthErrorMessage, AuthModalMode } from '../../core/auth/auth.models';
import { AuthModalComponent } from '../auth-modal/auth-modal';
import { ChangePasswordModalComponent } from '../change-password-modal/change-password-modal';

type AccountDrawerStatus = 'signed-out' | 'loading' | 'signed-in' | 'unauthorized' | 'error';

@Component({
  selector: 'app-account-drawer',
  imports: [AuthModalComponent, ChangePasswordModalComponent, ReactiveFormsModule],
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
  readonly isPasswordModalOpen = signal(false);
  readonly isEditingProfile = signal(false);
  readonly isSubmittingProfile = signal(false);
  readonly isLoggingOut = signal(false);
  readonly profileSuccessMessage = signal<string | null>(null);
  readonly passwordSuccessMessage = signal<string | null>(null);
  readonly profileServerError = signal<AuthErrorMessage | null>(null);
  private readonly closeDelayMs = 240;
  private readonly fb = inject(NonNullableFormBuilder);
  readonly profileForm = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    userSms: ['', [Validators.maxLength(20), Validators.pattern(/^$|^(?=.*\d)[+0-9() .-]+$/)]],
  });
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
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

    this.isPasswordModalOpen.set(false);
    this.isClosing.set(true);
    this.closeTimer = window.setTimeout(() => {
      this.isOpen.set(false);
      this.isClosing.set(false);
      this.closeTimer = null;
    }, this.closeDelayMs);
  }

  openPasswordModal(): void {
    this.passwordSuccessMessage.set(null);
    this.isPasswordModalOpen.set(true);
  }

  closePasswordModal(): void {
    this.isPasswordModalOpen.set(false);
  }

  onPasswordChanged(): void {
    this.passwordSuccessMessage.set('Password updated.');
  }

  logout(): void {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);

    this.authService
      .logout()
      .pipe(
        catchError(() => of(undefined)),
        finalize(() => this.isLoggingOut.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.authService.clearSession();
        this.resetSignedInState();
        this.status.set('signed-out');
        void this.router.navigate(['/']);
        this.close();
      });
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
    if (!this.isOpen() || this.isPasswordModalOpen() || this.authModalMode()) {
      return;
    }

    this.close();
  }

  private loadAccountDetails(): void {
    this.account.set(null);

    if (!this.authService.isSignedIn()) {
      this.resetSignedInState();
      this.status.set('signed-out');
      return;
    }

    this.status.set('loading');

    this.authService.getCurrentAccount().subscribe({
      next: (account) => {
        this.account.set(account);
        this.resetProfileForm(account);
        this.isEditingProfile.set(false);
        this.profileServerError.set(null);
        this.profileSuccessMessage.set(null);
        this.passwordSuccessMessage.set(null);
        this.status.set('signed-in');
      },
      error: (error: unknown) => {
        this.account.set(null);
        this.resetSignedInState();

        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.authService.clearSession();
          this.status.set('unauthorized');
          return;
        }

        if (error instanceof HttpErrorResponse && error.status === 0) {
          this.authService.clearSession();
          this.status.set('signed-out');
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

  private resetSignedInState(): void {
    this.isPasswordModalOpen.set(false);
    this.isEditingProfile.set(false);
    this.profileServerError.set(null);
    this.passwordSuccessMessage.set(null);
    this.profileSuccessMessage.set(null);
    this.account.set(null);
  }
}
