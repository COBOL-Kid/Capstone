import { HttpErrorResponse } from '@angular/common/http';

export interface ValidationErrorResponse {
  message: string;
  errors: FieldValidationError[];
}

export interface FieldValidationError {
  field: string;
  message: string;
}

export interface FieldErrorMessage {
  message: string;
  fieldMessages: string[];
}

export function isValidationErrorResponse(value: unknown): value is ValidationErrorResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    'message' in value &&
    'errors' in value &&
    Array.isArray((value as ValidationErrorResponse).errors)
  );
}

export function toFieldErrorMessage(
  error: HttpErrorResponse,
  fallbackMessage: string,
): FieldErrorMessage {
  if (isValidationErrorResponse(error.error)) {
    return {
      message: error.error.message,
      fieldMessages: error.error.errors.map((fieldError) => fieldError.message),
    };
  }

  if (typeof error.error === 'string' && error.error.trim().length > 0) {
    return { message: error.error, fieldMessages: [] };
  }

  return { message: fallbackMessage, fieldMessages: [] };
}
