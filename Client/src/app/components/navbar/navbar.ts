import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { AccountDrawerComponent } from '../account-drawer/account-drawer';

@Component({
  selector: 'app-navbar',
  imports: [AccountDrawerComponent, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  protected readonly homeLink = computed(() => (this.authService.isSignedIn() ? '/home' : '/'));
}
