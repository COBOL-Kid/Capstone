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

export function toFieldErrorMessage(body: unknown, fallbackMessage: string): FieldErrorMessage {
  if (isValidationErrorResponse(body)) {
    return {
      message: body.message,
      fieldMessages: body.errors.map((fieldError) => fieldError.message),
    };
  }

  if (typeof body === 'string' && body.trim().length > 0) {
    return { message: body, fieldMessages: [] };
  }

  return { message: fallbackMessage, fieldMessages: [] };
}

/** Maps null/undefined bodies (e.g. 204 No Content) to an empty array for list GETs. */
export function normalizeListResponse<T>(items: T[] | null | undefined): T[] {
  return items ?? [];
}
