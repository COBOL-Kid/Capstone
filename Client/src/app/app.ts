import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavbarComponent } from './components/navbar/navbar';
import { ToastHostComponent } from './components/toast-host/toast-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, ToastHostComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly title = signal('Client');
}
