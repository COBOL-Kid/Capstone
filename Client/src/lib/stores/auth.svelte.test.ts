import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as client from '$lib/api/client';
import { ApiError } from '$lib/api/client';
import type { AccountDetails, AuthenticationResponse } from '$lib/models/auth';
import { authStore } from './auth.svelte';

describe('authStore.validateSession', () => {
  const sampleAccount: AccountDetails = {
    userId: 1,
    email: 'user@example.com',
    firstName: 'Test',
    lastName: 'User',
    emailVerified: true,
    emailVerifiedAt: '2026-01-01T00:00:00Z',
    userSms: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  const sampleAuthResponse: AuthenticationResponse = {
    verificationRequired: false,
  };

  beforeEach(() => {
    authStore.clearSession();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    authStore.clearSession();
    vi.restoreAllMocks();
  });

  it('returns true immediately when session is active with cached account', async () => {
    const postSpy = vi.spyOn(client, 'apiPost');
    const getSpy = vi.spyOn(client, 'apiGet').mockResolvedValue(sampleAccount);

    // Hydrate initial account
    await authStore.getCurrentAccount();
    expect(authStore.isSignedIn).toBe(true);
    expect(getSpy).toHaveBeenCalledTimes(1);

    postSpy.mockClear();
    getSpy.mockClear();

    const result = await authStore.validateSession();
    expect(result).toBe(true);
    expect(postSpy).not.toHaveBeenCalled();
    expect(getSpy).not.toHaveBeenCalled();
  });

  it('refreshes and hydrates account on cold visits when token is valid', async () => {
    vi.spyOn(client, 'apiPost').mockImplementation(async (url: string) => {
      if (url.includes('/refresh')) {
        return sampleAuthResponse;
      }
      throw new Error(`Unexpected post url: ${url}`);
    });
    vi.spyOn(client, 'apiGet').mockImplementation(async (url: string) => {
      if (url.includes('/me')) {
        return sampleAccount;
      }
      throw new Error(`Unexpected get url: ${url}`);
    });

    const result = await authStore.validateSession();
    expect(result).toBe(true);
    expect(authStore.isSignedIn).toBe(true);
    expect(authStore.account).toEqual(sampleAccount);
  });

  it('keeps cookie-backed session when account hydration returns 403 (unverified user)', async () => {
    vi.spyOn(client, 'apiPost').mockImplementation(async (url: string) => {
      if (url.includes('/refresh')) {
        return sampleAuthResponse;
      }
      throw new Error(`Unexpected post url: ${url}`);
    });
    vi.spyOn(client, 'apiGet').mockImplementation(async (url: string) => {
      if (url.includes('/me')) {
        throw new ApiError(403, 'Forbidden');
      }
      throw new Error(`Unexpected get url: ${url}`);
    });

    const result = await authStore.validateSession();
    expect(result).toBe(true);
    expect(authStore.isSignedIn).toBe(true);
    expect(authStore.account).toBeNull();
  });

  it('clears session and returns false when account hydration returns 401', async () => {
    vi.spyOn(client, 'apiPost').mockImplementation(async (url: string) => {
      if (url.includes('/refresh')) {
        return sampleAuthResponse;
      }
      throw new Error(`Unexpected post url: ${url}`);
    });
    vi.spyOn(client, 'apiGet').mockImplementation(async (url: string) => {
      if (url.includes('/me')) {
        throw new ApiError(401, 'Unauthorized');
      }
      throw new Error(`Unexpected get url: ${url}`);
    });

    const result = await authStore.validateSession();
    expect(result).toBe(false);
    expect(authStore.isSignedIn).toBe(false);
    expect(authStore.account).toBeNull();
  });

  it('returns false when refresh fails with 401 on cold visits', async () => {
    vi.spyOn(client, 'apiPost').mockImplementation(async (url: string) => {
      if (url.includes('/refresh')) {
        throw new ApiError(401, 'Invalid refresh token');
      }
      throw new Error(`Unexpected post url: ${url}`);
    });
    const getSpy = vi.spyOn(client, 'apiGet');

    const result = await authStore.validateSession();
    expect(result).toBe(false);
    expect(authStore.isSignedIn).toBe(false);
    expect(getSpy).not.toHaveBeenCalled();
  });
});
