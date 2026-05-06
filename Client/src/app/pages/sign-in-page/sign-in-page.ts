import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-sign-in-page',
  templateUrl: './sign-in-page.html',
  styleUrl: './sign-in-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SignInPageComponent {}
