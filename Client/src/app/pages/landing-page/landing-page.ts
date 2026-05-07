import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
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
  private readonly routeData = toSignal(this.route.data, {
    initialValue: this.route.snapshot.data,
  });

  protected readonly authModalMode = computed<AuthModalMode | null>(() => {
    const mode = this.routeData()['authMode'];

    return mode === 'sign-in' || mode === 'sign-up' ? mode : null;
  });

  protected closeAuthModal(): void {
    void this.router.navigate(['/']);
  }
}
