import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, Observable, of, switchMap, tap, throwError } from 'rxjs';

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

export interface GetCurrentAccountOptions {
  forceRefresh?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly sessionActive = signal(false);
  readonly account = signal<AccountDetails | null>(null);
  readonly isSignedIn = computed(() => this.sessionActive());
  readonly isEmailVerified = computed(() => this.account()?.emailVerified ?? null);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = apiConfig.authUrl;
  private readonly accountApiBaseUrl = apiConfig.accountUrl;
  private readonly vehiclesStore = inject(UserVehiclesStore);

  register(request: RegisterRequest): Observable<AuthenticationResponse> {
    return this.http.post<AuthenticationResponse>(`${this.apiBaseUrl}/register`, request).pipe(
      map((response) => this.applyAuthResponse(response)),
      catchError((error) => this.handleAuthError(error)),
    );
  }

  login(request: AuthenticationRequest): Observable<AuthenticationResponse> {
    return this.http.post<AuthenticationResponse>(`${this.apiBaseUrl}/authenticate`, request).pipe(
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
      .post<AuthenticationResponse>(`${this.apiBaseUrl}/email-verification/verify`, request)
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
      )
      .pipe(
        map((response) => this.applyAuthResponse(response)),
        catchError((error) => this.handleAuthError(error)),
      );
  }

  refresh(): Observable<AuthenticationResponse> {
    return this.http.post<AuthenticationResponse>(`${this.apiBaseUrl}/refresh`, {}).pipe(
      map((response) => this.applyAuthResponse(response)),
      catchError((error) => this.handleAuthError(error)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/logout`, {}).pipe(
      map((response) => {
        this.clearSession();
        return response;
      }),
      catchError((error) => this.handleAuthError(error)),
    );
  }

  clearSession(): void {
    this.deactivateSession();
    this.vehiclesStore.reset();
  }

  validateSession(): Observable<boolean> {
    const hydrateAccount = (): Observable<boolean> =>
      this.getCurrentAccount().pipe(
        map(() => true),
        catchError((error: unknown) => {
          if (error instanceof HttpErrorResponse && error.status === 401) {
            this.clearSession();
            return of(false);
          }
          // Keep cookie-backed sessions when account hydration fails for other
          // reasons (for example a transient 403) so successful login is not
          // immediately undone by the auth guard.
          return of(this.sessionActive());
        }),
      );

    if (this.sessionActive()) {
      if (this.account() !== null) {
        return of(true);
      }
      return hydrateAccount();
    }

    return this.refresh().pipe(
      switchMap(() => hydrateAccount()),
      catchError(() => of(false)),
    );
  }

  getCurrentAccount(options: GetCurrentAccountOptions = {}): Observable<AccountDetails> {
    const cached = this.account();
    if (cached && !options.forceRefresh) {
      return of(cached);
    }

    return this.http.get<AccountDetails>(`${this.accountApiBaseUrl}/me`).pipe(
      tap((account) => {
        this.account.set(account);
        this.sessionActive.set(true);
      }),
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
      .post<VerifyAccountChangeResponse>(`${this.accountApiBaseUrl}/change-requests/verify`, {
        code,
      })
      .pipe(
        tap((response) => {
          this.activateSession();
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
    if (response.verificationRequired) {
      return response;
    }
    this.activateSession();
    this.account.set(null);
    return response;
  }

  private activateSession(): void {
    this.sessionActive.set(true);
  }

  private deactivateSession(): void {
    this.sessionActive.set(false);
    this.account.set(null);
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
