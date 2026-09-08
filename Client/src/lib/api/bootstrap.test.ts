import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  ensureAppBootstrapped,
  isAppBootstrapped,
  resetAppBootstrap,
  runAppBootstrap,
} from './bootstrap';
import * as client from './client';
import { authStore } from '$lib/stores/auth.svelte';

describe('app bootstrap', () => {
  const originalWindow = globalThis.window;

  beforeEach(() => {
    resetAppBootstrap();
    globalThis.window = {
      location: {
        origin: 'http://localhost:4200',
      },
    } as unknown as Window & typeof globalThis;
  });

  afterEach(() => {
    globalThis.window = originalWindow;
    vi.restoreAllMocks();
  });

  it('runs CSRF issuance before session validation', async () => {
    const callOrder: string[] = [];
    vi.spyOn(client, 'ensureCsrfToken').mockImplementation(async () => {
      callOrder.push('csrf');
    });
    vi.spyOn(authStore, 'validateSession').mockImplementation(async () => {
      callOrder.push('validateSession');
      return true;
    });

    await runAppBootstrap();

    expect(callOrder).toEqual(['csrf', 'validateSession']);
  });

  it('deduplicates concurrent bootstrap invocations', async () => {
    let validateCount = 0;
    vi.spyOn(client, 'ensureCsrfToken').mockResolvedValue();
    vi.spyOn(authStore, 'validateSession').mockImplementation(async () => {
      validateCount++;
      return true;
    });

    expect(isAppBootstrapped()).toBe(false);

    const [p1, p2, p3] = [
      ensureAppBootstrapped(),
      ensureAppBootstrapped(),
      ensureAppBootstrapped(),
    ];
    await Promise.all([p1, p2, p3]);

    expect(validateCount).toBe(1);
    expect(isAppBootstrapped()).toBe(true);
  });

  it('does not crash if CSRF or session validation fails on cold visits', async () => {
    vi.spyOn(client, 'ensureCsrfToken').mockRejectedValue(new Error('Network error'));
    vi.spyOn(authStore, 'validateSession').mockRejectedValue(new Error('Session error'));

    await expect(ensureAppBootstrapped()).resolves.toBeUndefined();
    expect(isAppBootstrapped()).toBe(true);
  });
});
