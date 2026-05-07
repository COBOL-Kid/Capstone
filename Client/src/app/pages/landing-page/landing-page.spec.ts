import { BehaviorSubject } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';

import { LandingPageComponent } from './landing-page';

describe('LandingPageComponent', () => {
  function configureRoute(data: Record<string, string>) {
    const routeData = new BehaviorSubject(data);

    TestBed.configureTestingModule({
      imports: [LandingPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: routeData.asObservable(),
            snapshot: { data },
          },
        },
      ],
    });

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    return router;
  }

  it('opens the sign-in modal for sign-in route data', () => {
    configureRoute({ authMode: 'sign-in' });

    const fixture = TestBed.createComponent(LandingPageComponent);
    fixture.detectChanges();

    const modal = fixture.nativeElement.querySelector('app-auth-modal');
    expect(modal).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Welcome back');
  });

  it('opens the sign-up modal for sign-up route data', () => {
    configureRoute({ authMode: 'sign-up' });

    const fixture = TestBed.createComponent(LandingPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-auth-modal')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Create your account');
  });

  it('navigates home when the modal closes', () => {
    const router = configureRoute({ authMode: 'sign-in' });

    const fixture = TestBed.createComponent(LandingPageComponent);
    fixture.detectChanges();

    const closeButton = fixture.nativeElement.querySelector(
      '.auth-modal__close',
    ) as HTMLButtonElement;
    closeButton.click();

    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
