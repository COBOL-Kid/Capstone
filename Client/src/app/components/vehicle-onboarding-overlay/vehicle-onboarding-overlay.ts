import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-vehicle-onboarding-overlay',
  standalone: true,
  templateUrl: './vehicle-onboarding-overlay.html',
  styleUrl: './vehicle-onboarding-overlay.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehicleOnboardingOverlayComponent {}
