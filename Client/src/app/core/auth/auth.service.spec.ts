import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { apiConfig } from '../api/api.config';
import { credentialsInterceptor } from '../http/credentials.interceptor';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('reports email verification as unknown before account details load', () => {
    expect(service.isEmailVerified()).toBeNull();
  });

  it('posts registration requests with credentials and activates the session', () => {
    const request = {
      firstname: 'Pat',
      lastname: 'Driver',
      email: 'pat@example.com',
      password: 'Password1!',
    };

    service.register(request).subscribe((response) => {
      expect(response.emailVerified).toBe(false);
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/register`);
    expect(authRequest.request.method).toBe('POST');
    expect(authRequest.request.withCredentials).toBe(true);

    authRequest.flush({ emailVerified: false });

    expect(service.isSignedIn()).toBe(true);
  });

  it('does not activate a session when login requires email verification', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe((response) => {
      expect(response.verificationRequired).toBe(true);
      expect(response.verificationChallenge).toBe('challenge-token');
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`);
    authRequest.flush({
      verificationRequired: true,
      verificationChallenge: 'challenge-token',
    });

    expect(service.isSignedIn()).toBe(false);
  });

  it('activates a session when login returns a session', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();

    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ emailVerified: true });

    expect(service.isSignedIn()).toBe(true);
  });

  it('posts verification and resend requests to the email verification endpoints', () => {
    service.verifyEmailCode('123456').subscribe();
    httpTesting
      .expectOne(`${apiConfig.authUrl}/email-verification/verify`)
      .flush({ emailVerified: true });

    service.resendEmailVerification().subscribe();
    httpTesting
      .expectOne(`${apiConfig.authUrl}/email-verification/resend`)
      .flush({ emailVerified: false });
  });

  it('completes sign-in verification and activates the session', () => {
    service.completeEmailVerificationSignIn('challenge-token', '123456').subscribe((response) => {
      expect(response.emailVerified).toBe(true);
    });

    const request = httpTesting.expectOne(
      `${apiConfig.authUrl}/email-verification/complete-sign-in`,
    );
    expect(request.request.body).toEqual({
      verificationChallenge: 'challenge-token',
      code: '123456',
    });
    request.flush({ emailVerified: true });

    expect(service.isSignedIn()).toBe(true);
  });

  it('gets current account details with email verification state', () => {
    service.getCurrentAccount().subscribe((account) => {
      expect(account.emailVerified).toBe(false);
      expect(service.isEmailVerified()).toBe(false);
    });

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    expect(accountRequest.request.withCredentials).toBe(true);
    accountRequest.flush({
      userId: 1,
      email: 'pat@example.com',
      firstName: 'Pat',
      lastName: 'Driver',
      userSms: null,
      emailVerified: false,
      emailVerifiedAt: null,
      createdAt: '2026-05-07T17:47:00Z',
      updatedAt: '2026-05-07T17:47:00Z',
    });

    expect(service.isSignedIn()).toBe(true);
  });

  it('clears the session when current account lookup returns unauthorized', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ emailVerified: true });

    service.getCurrentAccount().subscribe({
      error: (error) => {
        expect(error.status).toBe(401);
      },
    });

    httpTesting
      .expectOne(`${apiConfig.accountUrl}/me`)
      .flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(service.isSignedIn()).toBe(false);
  });

  it('restores the session from refresh when validating on page load', () => {
    service.validateSession().subscribe((valid) => {
      expect(valid).toBe(true);
    });

    const refreshRequest = httpTesting.expectOne(`${apiConfig.authUrl}/refresh`);
    expect(refreshRequest.request.withCredentials).toBe(true);
    refreshRequest.flush({ emailVerified: true });

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    accountRequest.flush({
      userId: 1,
      email: 'pat@example.com',
      firstName: 'Pat',
      lastName: 'Driver',
      userSms: null,
      emailVerified: true,
      emailVerifiedAt: '2026-01-02T03:04:00Z',
      createdAt: '2026-05-07T17:47:00Z',
      updatedAt: '2026-05-07T17:47:00Z',
    });

    expect(service.isSignedIn()).toBe(true);
  });

  it('keeps an active session when account hydration returns forbidden', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ emailVerified: true });

    service.validateSession().subscribe((valid) => {
      expect(valid).toBe(true);
    });

    httpTesting
      .expectOne(`${apiConfig.accountUrl}/me`)
      .flush('Forbidden', { status: 403, statusText: 'Forbidden' });

    expect(service.isSignedIn()).toBe(true);
  });

  it('clears the session when account hydration returns unauthorized during validation', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ emailVerified: true });

    service.validateSession().subscribe((valid) => {
      expect(valid).toBe(false);
    });

    httpTesting
      .expectOne(`${apiConfig.accountUrl}/me`)
      .flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(service.isSignedIn()).toBe(false);
  });

  it('maps invalid credential responses into a user-facing message', () => {
    service.login({ email: 'pat@example.com', password: 'wrong' }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Invalid account credentials');
      },
    });

    httpTesting
      .expectOne(`${apiConfig.authUrl}/authenticate`)
      .flush('Invalid account credentials', { status: 401, statusText: 'Unauthorized' });
  });

  it('surfaces server error text when registration fails with 500', () => {
    service
      .register({
        firstname: 'Pat',
        lastname: 'Driver',
        email: 'pat@example.com',
        password: 'Password1!',
      })
      .subscribe({
        error: (error) => {
          expect(error.message).toBe("Sometimes things just don't go as planned.");
        },
      });

    const request = httpTesting.expectOne(`${apiConfig.authUrl}/register`);
    request.flush("Sometimes things just don't go as planned.", {
      status: 500,
      statusText: 'Internal Server Error',
    });

    expect(service.isSignedIn()).toBe(false);
  });
});
