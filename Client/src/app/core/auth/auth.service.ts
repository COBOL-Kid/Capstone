import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, Observable, of, tap, throwError } from 'rxjs';

import {
  AccountDetails,
  AuthenticationRequest,
  AuthenticationResponse,
  AuthErrorMessage,
  ChangePasswordRequest,
  RegisterRequest,
  UpdateAccountRequest,
} from './auth.models';
import { apiConfig } from '../api/api.config';
import { toFieldErrorMessage } from '../http/http-error.util';
import { UserVehiclesStore } from '../vin/user-vehicles.store';

const authTokenStorageKey = 'honest-car.access-token';

export interface GetCurrentAccountOptions {
  forceRefresh?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly token = signal<string | null>(this.readStoredToken());
  readonly account = signal<AccountDetails | null>(null);
  readonly isSignedIn = computed(() => this.token() !== null);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = apiConfig.authUrl;
  private readonly accountApiBaseUrl = apiConfig.accountUrl;
  private readonly vehiclesStore = inject(UserVehiclesStore);

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
        this.clearSession();
        return response;
      }),
      catchError((error) => this.handleAuthError(error)),
    );
  }

  clearSession(): void {
    this.clearToken();
    this.vehiclesStore.reset();
  }

  validateSession(): Observable<boolean> {
    if (!this.isSignedIn()) {
      return of(false);
    }

    if (this.account() !== null) {
      return of(true);
    }

    return this.getCurrentAccount().pipe(
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      }),
    );
  }

  getCurrentAccount(options: GetCurrentAccountOptions = {}): Observable<AccountDetails> {
    const cached = this.account();
    if (cached && !options.forceRefresh) {
      return of(cached);
    }

    return this.http.get<AccountDetails>(`${this.accountApiBaseUrl}/me`).pipe(
      tap((account) => this.account.set(account)),
      catchError((error) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.clearSession();
        }
        return throwError(() => error);
      }),
    );
  }

  updateCurrentAccount(request: UpdateAccountRequest): Observable<AccountDetails> {
    return this.http.patch<AccountDetails>(`${this.accountApiBaseUrl}/me`, request).pipe(
      tap((account) => this.account.set(account)),
      catchError((error) => this.handleAuthError(error)),
    );
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http
      .post<void>(`${this.accountApiBaseUrl}/password`, request)
      .pipe(catchError((error) => this.handleAuthError(error)));
  }

  private storeToken(token: string): void {
    this.account.set(null);
    this.token.set(token);

    try {
      localStorage.setItem(authTokenStorageKey, token);
    } catch {
      // Token remains available in memory when browser storage is unavailable.
    }
  }

  private clearToken(): void {
    this.token.set(null);
    this.account.set(null);

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
    if (error.status === 401) {
      return { message: 'Invalid email or password.', fieldMessages: [] };
    }

    if (error.status === 409) {
      return { message: 'An account already exists for that email.', fieldMessages: [] };
    }

    return toFieldErrorMessage(error, 'Unable to complete the request. Please try again.');
  }
}
