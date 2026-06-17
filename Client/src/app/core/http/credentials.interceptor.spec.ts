import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiConfig, backendOrigin } from '../api/api.config';
import { credentialsInterceptor } from './credentials.interceptor';

describe('credentialsInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('sets withCredentials for same-origin API requests', () => {
    http.get(`${apiConfig.accountUrl}/me`).subscribe();

    const request = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    expect(request.request.withCredentials).toBe(true);
    request.flush({});
  });

  it('does not attach credentials to external origins', () => {
    http.get('https://evil.example.com/api/account/me').subscribe();

    const request = httpTesting.expectOne('https://evil.example.com/api/account/me');
    expect(request.request.withCredentials).toBe(false);
    request.flush({});
  });

  it('does not attach credentials to prefix-spoofed origins', () => {
    const spoofedOrigin = `${backendOrigin}.evil.test/api/account/me`;
    http.get(spoofedOrigin).subscribe();

    const request = httpTesting.expectOne(spoofedOrigin);
    expect(request.request.withCredentials).toBe(false);
    request.flush({});
  });
});
