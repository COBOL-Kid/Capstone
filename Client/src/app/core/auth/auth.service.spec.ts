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

  it('posts registration requests with credentials and stores the returned token', () => {
    const request = {
      firstname: 'Pat',
      lastname: 'Driver',
      email: 'pat@example.com',
      password: 'Password1!',
    };

    service.register(request).subscribe((response) => {
      expect(response.token).toBe('jwt-token');
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/register`);
    expect(authRequest.request.method).toBe('POST');
    expect(authRequest.request.body).toEqual(request);
    expect(authRequest.request.withCredentials).toBe(true);

    authRequest.flush({ token: 'jwt-token' });

    expect(service.token()).toBe('jwt-token');
    expect(localStorage.getItem('honest-car.access-token')).toBe('jwt-token');
  });

  it('posts login requests with credentials and stores the returned token', () => {
    const request = { email: 'pat@example.com', password: 'password' };

    service.login(request).subscribe();

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`);
    expect(authRequest.request.method).toBe('POST');
    expect(authRequest.request.body).toEqual(request);
    expect(authRequest.request.withCredentials).toBe(true);

    authRequest.flush({ token: 'login-token' });

    expect(service.token()).toBe('login-token');
  });

  it('gets current account details with the stored bearer token', () => {
    localStorage.setItem('honest-car.access-token', 'account-token');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);

    service.getCurrentAccount().subscribe((account) => {
      expect(account.email).toBe('pat@example.com');
    });

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    expect(accountRequest.request.method).toBe('GET');

    accountRequest.flush({
      userId: 1,
      email: 'pat@example.com',
      firstName: 'Pat',
      lastName: 'Driver',
      userSms: null,
      createdAt: '2026-05-07T17:47:00Z',
      updatedAt: '2026-05-07T17:47:00Z',
    });
  });

  it('does not attach an authorization header for account details when no token exists', () => {
    service.getCurrentAccount().subscribe();

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    expect(accountRequest.request.method).toBe('GET');
    expect(accountRequest.request.headers.has('Authorization')).toBe(false);

    accountRequest.flush({
      userId: 1,
      email: 'pat@example.com',
      firstName: 'Pat',
      lastName: 'Driver',
      userSms: null,
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

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    accountRequest.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(service.token()).toBeNull();
  });

  it('patches current account details with the stored bearer token', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ token: 'profile-token' });
    const request = {
      firstName: 'Pat',
      lastName: 'Driver',
      email: 'new@example.com',
      userSms: '+15551234567',
    };

    service.updateCurrentAccount(request).subscribe((account) => {
      expect(account.email).toBe('new@example.com');
    });

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    expect(accountRequest.request.method).toBe('PATCH');
    expect(accountRequest.request.body).toEqual(request);

    accountRequest.flush({
      userId: 1,
      email: 'new@example.com',
      firstName: 'Pat',
      lastName: 'Driver',
      userSms: '+15551234567',
      createdAt: '2026-05-07T17:47:00Z',
      updatedAt: '2026-05-08T17:47:00Z',
    });
  });

  it('posts password changes with the stored bearer token', () => {
    service.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ token: 'password-token' });
    const request = { currentPassword: 'old-secret', newPassword: 'new-secret' };

    service.changePassword(request).subscribe((response) => {
      expect(response).toBeNull();
    });

    const passwordRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/password`);
    expect(passwordRequest.request.method).toBe('POST');
    expect(passwordRequest.request.body).toEqual(request);

    passwordRequest.flush(null);
  });

  it('maps account update conflicts into user-facing errors', () => {
    service
      .updateCurrentAccount({
        firstName: 'Pat',
        lastName: 'Driver',
        email: 'taken@example.com',
        userSms: null,
      })
      .subscribe({
        error: (error) => {
          expect(error.message).toBe('An account already exists for that email.');
          expect(error.fieldMessages).toEqual([]);
        },
      });

    const accountRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    accountRequest.flush('Email is already in use', { status: 409, statusText: 'Conflict' });
  });

  it('maps password validation errors into user-facing field messages', () => {
    service.changePassword({ currentPassword: '', newPassword: 'short' }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Validation failed');
        expect(error.fieldMessages).toEqual(['Current password is required']);
      },
    });

    const passwordRequest = httpTesting.expectOne(`${apiConfig.accountUrl}/password`);
    passwordRequest.flush(
      {
        message: 'Validation failed',
        errors: [{ field: 'currentPassword', message: 'Current password is required' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
  });

  it('maps backend validation errors into user-facing field messages', () => {
    service.register({ firstname: '', lastname: '', email: 'bad', password: 'bad' }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Validation failed');
        expect(error.fieldMessages).toEqual(['Email must be valid']);
      },
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/register`);
    authRequest.flush(
      {
        message: 'Validation failed',
        errors: [{ field: 'email', message: 'Email must be valid' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
  });

  it('maps invalid credential responses into a user-facing message', () => {
    service.login({ email: 'pat@example.com', password: 'wrong' }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Invalid email or password.');
        expect(error.fieldMessages).toEqual([]);
      },
    });

    const authRequest = httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`);
    authRequest.flush('Invalid account credentials', { status: 401, statusText: 'Unauthorized' });
  });
});
