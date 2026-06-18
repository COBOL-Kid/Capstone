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
import { Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import {
  AccountChangeType,
  AccountDetails,
  AuthErrorMessage,
  AuthModalMode,
} from '../../core/auth/auth.models';
import { localDateIsoFromTimestamp } from '../../core/date/local-date';
import { ToastService } from '../../core/toast/toast.service';
import { AuthModalComponent } from '../auth-modal/auth-modal';
import { ChangeEmailModalComponent } from '../change-email-modal/change-email-modal';
import { ChangePasswordModalComponent } from '../change-password-modal/change-password-modal';
import { ChangeSmsModalComponent } from '../change-sms-modal/change-sms-modal';

type AccountDrawerStatus = 'signed-out' | 'loading' | 'signed-in' | 'unauthorized' | 'error';
type AccountChangeModal = AccountChangeType | null;

@Component({
  selector: 'app-account-drawer',
  imports: [
    AuthModalComponent,
    ChangeEmailModalComponent,
    ChangePasswordModalComponent,
    ChangeSmsModalComponent,
  ],
  templateUrl: './account-drawer.html',
  styleUrl: './account-drawer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountDrawerComponent {
  protected readonly formatAccountDate = localDateIsoFromTimestamp;
  readonly isOpen = signal(false);
  readonly isClosing = signal(false);
  readonly status = signal<AccountDrawerStatus>('signed-out');
  readonly account = signal<AccountDetails | null>(null);
  readonly authModalMode = signal<AuthModalMode | null>(null);
  readonly activeChangeModal = signal<AccountChangeModal>(null);
  readonly resumeChangeOnVerifyStep = signal(false);
  readonly isLoggingOut = signal(false);
  readonly registrationVerificationError = signal<AuthErrorMessage | null>(null);
  readonly isResendingRegistrationVerification = signal(false);
  private readonly closeDelayMs = 240;
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
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

    this.activeChangeModal.set(null);
    this.resumeChangeOnVerifyStep.set(false);
    this.isClosing.set(true);
    this.closeTimer = window.setTimeout(() => {
      this.isOpen.set(false);
      this.isClosing.set(false);
      this.closeTimer = null;
    }, this.closeDelayMs);
  }

  openChangeModal(changeType: AccountChangeType): void {
    this.resumeChangeOnVerifyStep.set(false);
    this.activeChangeModal.set(changeType);
  }

  closeChangeModal(): void {
    this.activeChangeModal.set(null);
    this.resumeChangeOnVerifyStep.set(false);
  }

  onEmailChanged(): void {
    this.toastService.success('Email updated.');
    this.refreshAccountAfterChange();
  }

  onSmsChanged(): void {
    this.toastService.success('SMS number updated.');
    this.refreshAccountAfterChange();
  }

  onPasswordChanged(): void {
    this.toastService.success('Password updated. Please sign in with your new password.');
    this.activeChangeModal.set(null);
    this.resumeChangeOnVerifyStep.set(false);
    this.close();
    void this.router.navigate(['/sign-in']);
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

  resendRegistrationVerification(): void {
    if (this.isResendingRegistrationVerification()) {
      return;
    }

    this.registrationVerificationError.set(null);
    this.isResendingRegistrationVerification.set(true);
    this.authService
      .resendEmailVerification()
      .pipe(
        finalize(() => this.isResendingRegistrationVerification.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (error: AuthErrorMessage) => this.registrationVerificationError.set(error),
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

    if (!this.isOpen()) {
      return;
    }

    if (this.authService.isSignedIn()) {
      void this.router.navigate(['/home']);
      this.loadAccountDetails(true);
      return;
    }

    this.resetSignedInState();
    this.status.set('signed-out');
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    if (!this.isOpen() || this.activeChangeModal() || this.authModalMode()) {
      return;
    }

    this.close();
  }

  private loadAccountDetails(forceRefresh = false): void {
    const cachedAccount = forceRefresh ? null : this.authService.account();
    if (cachedAccount) {
      this.setSignedInAccount(cachedAccount);
      this.checkPendingAccountChange();
      return;
    }

    this.account.set(null);

    if (!this.authService.isSignedIn()) {
      this.resetSignedInState();
      this.status.set('signed-out');
      return;
    }

    this.status.set('loading');

    this.authService
      .getCurrentAccount({ forceRefresh })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (account) => {
          this.setSignedInAccount(account);
          this.checkPendingAccountChange();
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

  private checkPendingAccountChange(): void {
    this.authService
      .getPendingAccountChange()
      .pipe(
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pending) => {
        if (!pending || this.activeChangeModal()) {
          return;
        }

        this.resumeChangeOnVerifyStep.set(true);
        this.activeChangeModal.set(pending.changeType);
      });
  }

  private refreshAccountAfterChange(): void {
    this.authService
      .getCurrentAccount({ forceRefresh: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (account) => this.account.set(account),
        error: () => undefined,
      });
  }

  private setSignedInAccount(account: AccountDetails): void {
    this.account.set(account);
    this.registrationVerificationError.set(null);
    this.status.set('signed-in');
  }

  private clearCloseTimer(): void {
    if (this.closeTimer === null) {
      return;
    }

    window.clearTimeout(this.closeTimer);
    this.closeTimer = null;
  }

  private resetSignedInState(): void {
    this.activeChangeModal.set(null);
    this.resumeChangeOnVerifyStep.set(false);
    this.registrationVerificationError.set(null);
    this.account.set(null);
  }
}
