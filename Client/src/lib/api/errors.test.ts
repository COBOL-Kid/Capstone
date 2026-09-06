import { describe, expect, it } from 'vitest';

import { isValidationErrorResponse, normalizeListResponse, toFieldErrorMessage } from './errors';

describe('isValidationErrorResponse', () => {
  it('detects validation payloads', () => {
    expect(
      isValidationErrorResponse({
        message: 'Validation failed',
        errors: [{ field: 'x', message: 'y' }],
      }),
    ).toBe(true);
  });

  it('rejects plain strings and null', () => {
    expect(isValidationErrorResponse('nope')).toBe(false);
    expect(isValidationErrorResponse(null)).toBe(false);
    expect(isValidationErrorResponse({ message: 'x' })).toBe(false);
  });
});

describe('toFieldErrorMessage', () => {
  it('maps validation bodies to message plus field messages', () => {
    expect(
      toFieldErrorMessage(
        { message: 'Validation failed', errors: [{ field: 'email', message: 'Invalid email' }] },
        'fallback',
      ),
    ).toEqual({ message: 'Validation failed', fieldMessages: ['Invalid email'] });
  });

  it('passes through non-empty strings', () => {
    expect(toFieldErrorMessage('Server says no', 'fallback')).toEqual({
      message: 'Server says no',
      fieldMessages: [],
    });
  });

  it('falls back for empty bodies', () => {
    expect(toFieldErrorMessage(null, 'fallback')).toEqual({
      message: 'fallback',
      fieldMessages: [],
    });
    expect(toFieldErrorMessage('   ', 'fallback')).toEqual({
      message: 'fallback',
      fieldMessages: [],
    });
  });
});

describe('normalizeListResponse', () => {
  it('maps null and undefined to an empty array', () => {
    expect(normalizeListResponse(null)).toEqual([]);
    expect(normalizeListResponse(undefined)).toEqual([]);
  });

  it('passes arrays through', () => {
    expect(normalizeListResponse([1, 2])).toEqual([1, 2]);
  });
});
