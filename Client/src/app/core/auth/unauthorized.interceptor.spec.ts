import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiConfig } from '../api/api.config';
import { credentialsInterceptor } from '../http/credentials.interceptor';
import { unauthorizedInterceptor } from './unauthorized.interceptor';
import { AuthService } from './auth.service';

describe('unauthorizedInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor, unauthorizedInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('clears the session on 401 from protected APIs when signed in', () => {
    authService.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ emailVerified: true });
    expect(authService.isSignedIn()).toBe(true);

    http.get(`${apiConfig.accountUrl}/me`).subscribe({ error: () => undefined });
    httpTesting
      .expectOne(`${apiConfig.accountUrl}/me`)
      .flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authService.isSignedIn()).toBe(false);
  });

  it('does not clear the session for 401 on login', () => {
    authService.login({ email: 'pat@example.com', password: 'wrong' }).subscribe({
      error: () => undefined,
    });

    httpTesting
      .expectOne(`${apiConfig.authUrl}/authenticate`)
      .flush('Invalid account credentials', { status: 401, statusText: 'Unauthorized' });

    expect(authService.isSignedIn()).toBe(false);
  });
});
