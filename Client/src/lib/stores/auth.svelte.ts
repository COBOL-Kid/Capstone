import { ApiError, apiGet, apiPatch, apiPost, onUnauthorized } from '$lib/api/client';
import { apiConfig } from '$lib/api/config';
import { toFieldErrorMessage, type FieldErrorMessage } from '$lib/api/errors';
import type {
  AccountChangeInitiatedResponse,
  AccountDetails,
  AuthErrorMessage,
  AuthenticationRequest,
  AuthenticationResponse,
  ChangePasswordRequest,
  CompleteEmailVerificationRequest,
  InitiateAccountChangeRequest,
  PendingAccountChange,
  RegisterRequest,
  UpdateAccountRequest,
  VerifyAccountChangeResponse,
  VerifyEmailRequest,
} from '$lib/models/auth';
import { vehiclesStore } from './vehicles.svelte';

export interface GetCurrentAccountOptions {
  forceRefresh?: boolean;
}

function toAuthErrorMessage(error: ApiError): AuthErrorMessage {
  const body = error.body;
  if (error.status === 401) {
    if (typeof body === 'string' && body.trim()) {
      return { message: body, fieldMessages: [] };
    }
    return { message: 'Invalid email or password.', fieldMessages: [] };
  }

  if (error.status === 404 && typeof body === 'string') {
    return { message: body, fieldMessages: [] };
  }

  if (error.status === 409) {
    return { message: 'An account already exists for that email.', fieldMessages: [] };
  }

  if (error.status === 400 && typeof body === 'string') {
    return { message: body, fieldMessages: [] };
  }

  if (error.status === 503 && typeof body === 'string') {
    return { message: body, fieldMessages: [] };
  }

  return toFieldErrorMessage(body, 'Unable to complete the request. Please try again.');
}

function toAuthError(error: unknown): FieldErrorMessage {
  if (error instanceof ApiError) {
    return toAuthErrorMessage(error);
  }
  return { message: 'Unable to complete the request. Please try again.', fieldMessages: [] };
}

class AuthStore {
  private sessionState = $state(false);
  private accountState = $state<AccountDetails | null>(null);

  readonly sessionActive = $derived(this.sessionState);
  readonly account = $derived(this.accountState);
  readonly isSignedIn = $derived(this.sessionState);
  readonly isEmailVerified = $derived(this.accountState?.emailVerified ?? null);

  constructor() {
    // Mirrors Angular's unauthorizedInterceptor: a 401 on any same-origin
    // non-auth request clears a locally active session.
    onUnauthorized(() => {
      if (this.sessionState) {
        this.clearSession();
      }
    });
  }

  async register(request: RegisterRequest): Promise<AuthenticationResponse> {
    try {
      const response = await apiPost<AuthenticationResponse>(
        `${apiConfig.authUrl}/register`,
        request,
      );
      return this.applyAuthResponse(response);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async login(request: AuthenticationRequest): Promise<AuthenticationResponse> {
    try {
      const response = await apiPost<AuthenticationResponse>(
        `${apiConfig.authUrl}/authenticate`,
        request,
      );
      return this.applyAuthResponse(response);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async resendEmailVerification(): Promise<AuthenticationResponse> {
    try {
      const response = await apiPost<AuthenticationResponse>(
        `${apiConfig.authUrl}/email-verification/resend`,
        {},
      );
      return this.applyAuthResponse(response);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async verifyEmailCode(code: string): Promise<AuthenticationResponse> {
    const request: VerifyEmailRequest = { code };
    try {
      const response = await apiPost<AuthenticationResponse>(
        `${apiConfig.authUrl}/email-verification/verify`,
        request,
      );
      return this.applyAuthResponse(response);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async completeEmailVerificationSignIn(
    verificationChallenge: string,
    code: string,
  ): Promise<AuthenticationResponse> {
    const request: CompleteEmailVerificationRequest = { verificationChallenge, code };
    try {
      const response = await apiPost<AuthenticationResponse>(
        `${apiConfig.authUrl}/email-verification/complete-sign-in`,
        request,
      );
      return this.applyAuthResponse(response);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async refresh(): Promise<AuthenticationResponse> {
    try {
      const response = await apiPost<AuthenticationResponse>(`${apiConfig.authUrl}/refresh`, {});
      return this.applyAuthResponse(response);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async logout(): Promise<void> {
    try {
      await apiPost<void>(`${apiConfig.authUrl}/logout`, {});
    } catch (error) {
      throw toAuthError(error);
    }
    this.clearSession();
  }

  clearSession(): void {
    this.sessionState = false;
    this.accountState = null;
    vehiclesStore.reset();
  }

  async validateSession(): Promise<boolean> {
    const hydrateAccount = async (): Promise<boolean> => {
      try {
        await this.getCurrentAccount();
        return true;
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          this.clearSession();
          return false;
        }
        // Keep cookie-backed sessions when account hydration fails for other
        // reasons (for example a transient 403) so successful login is not
        // immediately undone by the route guard.
        return this.sessionState;
      }
    };

    if (this.sessionState) {
      if (this.accountState !== null) {
        return true;
      }
      return hydrateAccount();
    }

    try {
      await this.refresh();
    } catch {
      return false;
    }
    return hydrateAccount();
  }

  async getCurrentAccount(options: GetCurrentAccountOptions = {}): Promise<AccountDetails> {
    const cached = this.accountState;
    if (cached && !options.forceRefresh) {
      return cached;
    }

    try {
      const account = await apiGet<AccountDetails>(`${apiConfig.accountUrl}/me`);
      this.accountState = account;
      this.sessionState = true;
      return account;
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        this.clearSession();
      }
      throw error;
    }
  }

  async updateCurrentAccount(request: UpdateAccountRequest): Promise<AccountDetails> {
    try {
      const account = await apiPatch<AccountDetails>(`${apiConfig.accountUrl}/me`, request);
      this.accountState = account;
      return account;
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async changePassword(request: ChangePasswordRequest): Promise<void> {
    try {
      await apiPost<void>(`${apiConfig.accountUrl}/password`, request);
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async initiateAccountChange(
    request: InitiateAccountChangeRequest,
  ): Promise<AccountChangeInitiatedResponse> {
    try {
      return await apiPost<AccountChangeInitiatedResponse>(
        `${apiConfig.accountUrl}/change-requests`,
        request,
      );
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async verifyAccountChange(code: string): Promise<VerifyAccountChangeResponse> {
    try {
      const response = await apiPost<VerifyAccountChangeResponse>(
        `${apiConfig.accountUrl}/change-requests/verify`,
        { code },
      );
      if (response.changeType === 'PASSWORD') {
        this.clearSession();
        return response;
      }
      this.sessionState = true;
      this.accountState = response.account;
      return response;
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async resendAccountChangeCode(): Promise<AccountChangeInitiatedResponse> {
    try {
      return await apiPost<AccountChangeInitiatedResponse>(
        `${apiConfig.accountUrl}/change-requests/resend`,
        {},
      );
    } catch (error) {
      throw toAuthError(error);
    }
  }

  async getPendingAccountChange(): Promise<PendingAccountChange | null> {
    try {
      const response = await apiGet<PendingAccountChange | null>(
        `${apiConfig.accountUrl}/change-requests/pending`,
      );
      return response;
    } catch (error) {
      throw toAuthError(error);
    }
  }

  private applyAuthResponse(response: AuthenticationResponse): AuthenticationResponse {
    if (response.verificationRequired) {
      return response;
    }
    this.sessionState = true;
    this.accountState = null;
    return response;
  }
}

export const authStore = new AuthStore();
