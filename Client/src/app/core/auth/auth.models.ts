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
  token: string;
}

export interface AccountDetails {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  userSms: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuthErrorMessage {
  message: string;
  fieldMessages: string[];
}

export interface ValidationErrorResponse {
  message: string;
  errors: FieldValidationError[];
}

export interface FieldValidationError {
  field: string;
  message: string;
}
