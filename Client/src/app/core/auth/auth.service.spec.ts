import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

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

    const authRequest = httpTesting.expectOne('http://localhost:8080/api/auth/register');
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

    const authRequest = httpTesting.expectOne('http://localhost:8080/api/auth/authenticate');
    expect(authRequest.request.method).toBe('POST');
    expect(authRequest.request.body).toEqual(request);
    expect(authRequest.request.withCredentials).toBe(true);

    authRequest.flush({ token: 'login-token' });

    expect(service.token()).toBe('login-token');
  });

  it('maps backend validation errors into user-facing field messages', () => {
    service.register({ firstname: '', lastname: '', email: 'bad', password: 'bad' }).subscribe({
      error: (error) => {
        expect(error.message).toBe('Validation failed');
        expect(error.fieldMessages).toEqual(['Email must be valid']);
      },
    });

    const authRequest = httpTesting.expectOne('http://localhost:8080/api/auth/register');
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
        expect(error.message).toBe('Invalid account credentials');
        expect(error.fieldMessages).toEqual([]);
      },
    });

    const authRequest = httpTesting.expectOne('http://localhost:8080/api/auth/authenticate');
    authRequest.flush('Invalid account credentials', { status: 401, statusText: 'Unauthorized' });
  });
});
