import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AccountDrawerComponent } from '../account-drawer/account-drawer';

@Component({
  selector: 'app-navbar',
  imports: [AccountDrawerComponent, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {}
