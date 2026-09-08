import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as bootstrap from '$lib/api/bootstrap';
import { authStore } from '$lib/stores/auth.svelte';
import { load as homeLoad } from './+page';
import { load as vehicleLoad } from '../vehicles/[vin]/+page';

describe('guarded route load', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('redirects /home to /sign-in when session is invalid', async () => {
    vi.spyOn(bootstrap, 'ensureAppBootstrapped').mockResolvedValue();
    vi.spyOn(authStore, 'validateSession').mockResolvedValue(false);

    try {
      await homeLoad();
      expect.fail('Expected redirect to be thrown');
    } catch (err: unknown) {
      expect(err).toMatchObject({
        status: 302,
        location: '/sign-in',
      });
    }
  });

  it('allows /home when session is valid', async () => {
    vi.spyOn(bootstrap, 'ensureAppBootstrapped').mockResolvedValue();
    vi.spyOn(authStore, 'validateSession').mockResolvedValue(true);

    const result = await homeLoad();
    expect(result).toEqual({});
  });

  it('ensures bootstrap runs before checking session in /home', async () => {
    const sequence: string[] = [];
    vi.spyOn(bootstrap, 'ensureAppBootstrapped').mockImplementation(async () => {
      sequence.push('bootstrap');
    });
    vi.spyOn(authStore, 'validateSession').mockImplementation(async () => {
      sequence.push('validateSession');
      return true;
    });

    await homeLoad();
    expect(sequence).toEqual(['bootstrap', 'validateSession']);
  });

  it('redirects /vehicles/[vin] to /sign-in when session is invalid', async () => {
    vi.spyOn(bootstrap, 'ensureAppBootstrapped').mockResolvedValue();
    vi.spyOn(authStore, 'validateSession').mockResolvedValue(false);

    try {
      await vehicleLoad();
      expect.fail('Expected redirect to be thrown');
    } catch (err: unknown) {
      expect(err).toMatchObject({
        status: 302,
        location: '/sign-in',
      });
    }
  });

  it('allows /vehicles/[vin] when session is valid', async () => {
    vi.spyOn(bootstrap, 'ensureAppBootstrapped').mockResolvedValue();
    vi.spyOn(authStore, 'validateSession').mockResolvedValue(true);

    const result = await vehicleLoad();
    expect(result).toEqual({});
  });
});
