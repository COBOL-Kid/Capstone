import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { apiConfig } from '../api/api.config';
import { credentialsInterceptor } from '../http/credentials.interceptor';
import { AuthService } from '../auth/auth.service';
import { AppBootstrapService } from './app-bootstrap.service';

describe('AppBootstrapService', () => {
  let service: AppBootstrapService;
  let httpTesting: HttpTestingController;
  let validateSession: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    validateSession = vi.fn().mockReturnValue(of(false));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { validateSession },
        },
      ],
    });

    service = TestBed.inject(AppBootstrapService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('fetches the CSRF cookie then validates the session', () => {
    let completed = false;

    service.run().subscribe(() => {
      completed = true;
    });

    const csrfRequest = httpTesting.expectOne(`${apiConfig.authUrl}/csrf`);
    expect(csrfRequest.request.method).toBe('GET');
    expect(csrfRequest.request.withCredentials).toBe(true);
    csrfRequest.flush('');

    expect(validateSession).toHaveBeenCalled();
    expect(completed).toBe(true);
  });

  it('still validates the session when the CSRF request fails', () => {
    let completed = false;

    service.run().subscribe(() => {
      completed = true;
    });

    const csrfRequest = httpTesting.expectOne(`${apiConfig.authUrl}/csrf`);
    csrfRequest.flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });

    expect(validateSession).toHaveBeenCalled();
    expect(completed).toBe(true);
  });

  it('completes when session validation fails', () => {
    validateSession.mockReturnValue(throwError(() => new Error('session failed')));
    let completed = false;

    service.run().subscribe(() => {
      completed = true;
    });

    const csrfRequest = httpTesting.expectOne(`${apiConfig.authUrl}/csrf`);
    csrfRequest.flush('');

    expect(validateSession).toHaveBeenCalled();
    expect(completed).toBe(true);
  });
});
