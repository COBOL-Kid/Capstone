import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

import { AuthModalComponent } from '../../components/auth-modal/auth-modal';
import { AuthModalMode } from '../../core/auth/auth.models';

@Component({
  selector: 'app-landing-page',
  imports: [NgOptimizedImage, RouterLink, AuthModalComponent],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authModalCloseDelayMs = 240;
  private readonly routeData = toSignal(this.route.data, {
    initialValue: this.route.snapshot.data,
  });
  protected readonly isAuthModalClosing = signal(false);

  protected readonly authModalMode = computed<AuthModalMode | null>(() => {
    const mode = this.routeData()['authMode'];

    return mode === 'sign-in' || mode === 'sign-up' ? mode : null;
  });

  constructor() {
    effect(() => {
      if (this.authModalMode()) {
        this.isAuthModalClosing.set(false);
      }
    });
  }

  protected closeAuthModal(): void {
    if (this.isAuthModalClosing()) {
      return;
    }

    this.isAuthModalClosing.set(true);
    window.setTimeout(() => {
      void this.router.navigate(['/']);
    }, this.authModalCloseDelayMs);
  }
}
