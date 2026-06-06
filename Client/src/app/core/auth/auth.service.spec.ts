import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { apiConfig } from '../api/api.config';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('reports email verification as unknown before account details load', () => {
    expect(service.isEmailVerified()).toBeNull();
  });

  it('posts registration requests with credentials and stores the returned token', () => {
    const request = {
      firstname: 'Pat',
      lastname: 'Driver',
      email: 'pat@example.com',
      password: 'Password1!',
    };

    service.register(request).subscribe((response) => {
      expect(response.token).toBe('jwt-token');
      expect(response.emailVerified).toBe(false);
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/register`);
    expect(authRequest.request.method).toBe('POST');
    expect(authRequest.request.withCredentials).toBe(true);

    authRequest.flush({ token: 'jwt-token', emailVerified: false });

    expect(service.token()).toBe('jwt-token');
    expect(localStorage.getItem('honest-car.access-token')).toBe('jwt-token');
  });

  it('does not store a token when login requires email verification', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe((response) => {
      expect(response.verificationRequired).toBe(true);
      expect(response.verificationChallenge).toBe('challenge-token');
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`);
    authRequest.flush({
      verificationRequired: true,
      verificationChallenge: 'challenge-token',
    });

    expect(service.token()).toBeNull();
  });

  it('stores a token when login returns a session', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();

    httpTesting
      .expectOne(`${apiConfig.authUrl}/authenticate`)
      .flush({ token: 'login-token', emailVerified: true });

    expect(service.token()).toBe('login-token');
  });

  it('posts verification and resend requests to the email verification endpoints', () => {
    service.verifyEmailCode('123456').subscribe();
    httpTesting
      .expectOne(`${apiConfig.authUrl}/email-verification/verify`)
      .flush({ token: 'verified-token', emailVerified: true });

    service.resendEmailVerification().subscribe();
    httpTesting
      .expectOne(`${apiConfig.authUrl}/email-verification/resend`)
      .flush({ emailVerified: false });
  });

  it('completes sign-in verification and stores the returned token', () => {
    service.completeEmailVerificationSignIn('challenge-token', '123456').subscribe((response) => {
      expect(response.token).toBe('session-token');
    });

    const request = httpTesting.expectOne(
      `${apiConfig.authUrl}/email-verification/complete-sign-in`,
    );
    expect(request.request.body).toEqual({
      verificationChallenge: 'challenge-token',
      code: '123456',
    });
    request.flush({ token: 'session-token', emailVerified: true });

    expect(service.token()).toBe('session-token');
  });

  it('gets current account details with email verification state', () => {
    localStorage.setItem('honest-car.access-token', 'account-token');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);

    service.getCurrentAccount().subscribe((account) => {
      expect(account.emailVerified).toBe(false);
      expect(service.isEmailVerified()).toBe(false);
    });

    httpTesting.expectOne(`${apiConfig.accountUrl}/me`).flush({
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
  });

  it('clears the session when current account lookup returns unauthorized', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ token: 'login-token' });

    service.getCurrentAccount().subscribe({
      error: (error) => {
        expect(error.status).toBe(401);
      },
    });

    httpTesting
      .expectOne(`${apiConfig.accountUrl}/me`)
      .flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(service.token()).toBeNull();
  });

  it('maps invalid credential responses into a user-facing message', () => {
    service.login({ email: 'pat@example.com', password: 'wrong' }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Invalid email or password.');
      },
    });

    httpTesting
      .expectOne(`${apiConfig.authUrl}/authenticate`)
      .flush('Invalid account credentials', { status: 401, statusText: 'Unauthorized' });
  });
});
