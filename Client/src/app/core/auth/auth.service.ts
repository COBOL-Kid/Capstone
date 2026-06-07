import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, Observable, of, tap, throwError } from 'rxjs';

import {
  AccountChangeInitiatedResponse,
  AccountDetails,
  AuthenticationRequest,
  AuthenticationResponse,
  AuthErrorMessage,
  ChangePasswordRequest,
  CompleteEmailVerificationRequest,
  InitiateAccountChangeRequest,
  PendingAccountChange,
  RegisterRequest,
  UpdateAccountRequest,
  VerifyAccountChangeResponse,
  VerifyEmailRequest,
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
  readonly isEmailVerified = computed(() => this.account()?.emailVerified ?? null);
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
        map((response) => this.applyAuthResponse(response)),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  login(request: AuthenticationRequest): Observable<AuthenticationResponse> {
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/authenticate`, request, {
        withCredentials: true,
      })
      .pipe(
        map((response) => this.applyAuthResponse(response)),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  resendEmailVerification(): Observable<AuthenticationResponse> {
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/email-verification/resend`, {}, {})
      .pipe(
        map((response) => this.applyAuthResponse(response)),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  verifyEmailCode(code: string): Observable<AuthenticationResponse> {
    const request: VerifyEmailRequest = { code };
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/email-verification/verify`, request, {
        withCredentials: true,
      })
      .pipe(
        map((response) => this.applyAuthResponse(response)),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  completeEmailVerificationSignIn(
    verificationChallenge: string,
    code: string,
  ): Observable<AuthenticationResponse> {
    const request: CompleteEmailVerificationRequest = { verificationChallenge, code };
    return this.http
      .post<AuthenticationResponse>(
        `${this.apiBaseUrl}/email-verification/complete-sign-in`,
        request,
        { withCredentials: true },
      )
      .pipe(
        map((response) => this.applyAuthResponse(response)),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  refresh(): Observable<AuthenticationResponse> {
    return this.http
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/refresh`, {}, { withCredentials: true })
      .pipe(
        map((response) => this.applyAuthResponse(response)),
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

  initiateAccountChange(
    request: InitiateAccountChangeRequest,
  ): Observable<AccountChangeInitiatedResponse> {
    return this.http
      .post<AccountChangeInitiatedResponse>(`${this.accountApiBaseUrl}/change-requests`, request)
      .pipe(catchError((error) => this.handleAuthError(error)));
  }

  verifyAccountChange(code: string): Observable<VerifyAccountChangeResponse> {
    return this.http
      .post<VerifyAccountChangeResponse>(
        `${this.accountApiBaseUrl}/change-requests/verify`,
        { code },
        { withCredentials: true },
      )
      .pipe(
        tap((response) => {
          if (response.token) {
            this.storeToken(response.token);
          }
          this.account.set(response.account);
        }),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  resendAccountChangeCode(): Observable<AccountChangeInitiatedResponse> {
    return this.http
      .post<AccountChangeInitiatedResponse>(`${this.accountApiBaseUrl}/change-requests/resend`, {})
      .pipe(catchError((error) => this.handleAuthError(error)));
  }

  getPendingAccountChange(): Observable<PendingAccountChange | null> {
    return this.http
      .get<PendingAccountChange>(`${this.accountApiBaseUrl}/change-requests/pending`, {
        observe: 'response',
      })
      .pipe(
        map((response) => (response.status === 204 ? null : (response.body ?? null))),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  private applyAuthResponse(response: AuthenticationResponse): AuthenticationResponse {
    if (response.token) {
      this.storeToken(response.token);
    }
    return response;
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
      if (typeof error.error === 'string' && error.error.trim()) {
        return { message: error.error, fieldMessages: [] };
      }
      return { message: 'Invalid email or password.', fieldMessages: [] };
    }

    if (error.status === 404 && typeof error.error === 'string') {
      return { message: error.error, fieldMessages: [] };
    }

    if (error.status === 409) {
      return { message: 'An account already exists for that email.', fieldMessages: [] };
    }

    if (error.status === 400 && typeof error.error === 'string') {
      return { message: error.error, fieldMessages: [] };
    }

    if (error.status === 503 && typeof error.error === 'string') {
      return { message: error.error, fieldMessages: [] };
    }

    return toFieldErrorMessage(error, 'Unable to complete the request. Please try again.');
  }
}
