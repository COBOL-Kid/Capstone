import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';

import {
  AuthenticationRequest,
  AuthenticationResponse,
  AuthErrorMessage,
  RegisterRequest,
  ValidationErrorResponse,
} from './auth.models';

const authTokenStorageKey = 'honest-car.access-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly token = signal<string | null>(this.readStoredToken());
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = 'http://localhost:8080/api/auth';

  register(request: RegisterRequest): Observable<AuthenticationResponse> {
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/register`, request, {
        withCredentials: true,
      })
      .pipe(
        map((response) => {
          this.storeToken(response.token);
          return response;
        }),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  login(request: AuthenticationRequest): Observable<AuthenticationResponse> {
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/authenticate`, request, {
        withCredentials: true,
      })
      .pipe(
        map((response) => {
          this.storeToken(response.token);
          return response;
        }),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  refresh(): Observable<AuthenticationResponse> {
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/refresh`, {}, { withCredentials: true })
      .pipe(
        map((response) => {
          this.storeToken(response.token);
          return response;
        }),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/logout`, {}, { withCredentials: true }).pipe(
      map((response) => {
        this.clearToken();
        return response;
      }),
      catchError((error) => this.handleAuthError(error)),
    );
  }

  private storeToken(token: string): void {
    this.token.set(token);

    try {
      localStorage.setItem(authTokenStorageKey, token);
    } catch {
      // Token remains available in memory when browser storage is unavailable.
    }
  }

  private clearToken(): void {
    this.token.set(null);

    try {
      localStorage.removeItem(authTokenStorageKey);
    } catch {
      // Nothing else to clear when browser storage is unavailable.
    }
  }

  private readStoredToken(): string | null {
    try {
      return localStorage.getItem(authTokenStorageKey);
    } catch {
      return null;
    }
  }

  private handleAuthError(error: unknown): Observable<never> {
    if (!(error instanceof HttpErrorResponse)) {
      return throwError(() => ({
        message: 'Unable to complete the request. Please try again.',
        fieldMessages: [],
      }));
    }

    return throwError(() => this.toAuthErrorMessage(error));
  }

  private toAuthErrorMessage(error: HttpErrorResponse): AuthErrorMessage {
    if (this.isValidationErrorResponse(error.error)) {
      return {
        message: error.error.message,
        fieldMessages: error.error.errors.map((fieldError) => fieldError.message),
      };
    }

    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return { message: error.error, fieldMessages: [] };
    }

    if (error.status === 401) {
      return { message: 'Invalid email or password.', fieldMessages: [] };
    }

    if (error.status === 409) {
      return { message: 'An account already exists for that email.', fieldMessages: [] };
    }

    return { message: 'Unable to complete the request. Please try again.', fieldMessages: [] };
  }

  private isValidationErrorResponse(value: unknown): value is ValidationErrorResponse {
    return (
      typeof value === 'object' &&
      value !== null &&
      'message' in value &&
      'errors' in value &&
      Array.isArray((value as ValidationErrorResponse).errors)
    );
  }
}
