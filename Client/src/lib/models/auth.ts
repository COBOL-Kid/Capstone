import type { FieldErrorMessage } from '$lib/api/errors';

export type AuthModalMode = 'sign-in' | 'sign-up';

export interface RegisterRequest {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
}

export interface AuthenticationRequest {
  email: string;
  password: string;
}

export interface AuthenticationResponse {
  token?: string;
  emailVerified?: boolean;
  verificationRequired?: boolean;
  verificationChallenge?: string;
}

export interface VerifyEmailRequest {
  code: string;
}

export interface CompleteEmailVerificationRequest {
  verificationChallenge: string;
  code: string;
}

export interface AccountDetails {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  userSms: string | null;
  emailVerified: boolean;
  emailVerifiedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateAccountRequest {
  firstName: string;
  lastName: string;
}

export type AccountChangeType = 'EMAIL' | 'PASSWORD' | 'SMS';

export interface InitiateAccountChangeRequest {
  changeType: AccountChangeType;
  newEmail?: string | null;
  currentPassword?: string | null;
  newPassword?: string | null;
  userSms?: string | null;
}

export interface AccountChangeInitiatedResponse {
  changeType: AccountChangeType;
  expiresInMinutes: number;
}

export interface PendingAccountChange {
  changeType: AccountChangeType;
  expiresInMinutes: number;
}

export interface VerifyAccountChangeResponse {
  account: AccountDetails;
  token?: string;
  emailVerified?: boolean;
  changeType: AccountChangeType;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export type AuthErrorMessage = FieldErrorMessage;
