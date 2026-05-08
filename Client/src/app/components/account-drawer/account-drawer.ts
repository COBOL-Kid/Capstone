import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/auth/auth.service';
import { AccountDetails, AuthModalMode } from '../../core/auth/auth.models';
import { AuthModalComponent } from '../auth-modal/auth-modal';

type AccountDrawerStatus = 'signed-out' | 'loading' | 'signed-in' | 'unauthorized' | 'error';

@Component({
  selector: 'app-account-drawer',
  imports: [AuthModalComponent],
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
  private readonly closeDelayMs = 240;
  private readonly authService = inject(AuthService);
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

    this.isClosing.set(true);
    this.closeTimer = window.setTimeout(() => {
      this.isOpen.set(false);
      this.isClosing.set(false);
      this.closeTimer = null;
    }, this.closeDelayMs);
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
      this.status.set('signed-out');
      return;
    }

    this.status.set('loading');

    this.authService.getCurrentAccount().subscribe({
      next: (account) => {
        this.account.set(account);
        this.status.set('signed-in');
      },
      error: (error: unknown) => {
        this.account.set(null);

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
}
