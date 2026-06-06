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
  email: string;
  userSms: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

import { FieldErrorMessage } from '../http/http-error.util';

export type AuthErrorMessage = FieldErrorMessage;
