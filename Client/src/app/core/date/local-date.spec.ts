import { afterEach, describe, expect, it, vi } from 'vitest';

import { localDateIso, localDateIsoFromTimestamp } from './local-date';

describe('localDateIso', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('formats the local calendar date, not UTC', () => {
    vi.stubEnv('TZ', 'America/Los_Angeles');

    const eveningLocal = new Date(2026, 4, 28, 22, 0, 0);

    expect(localDateIso(eveningLocal)).toBe('2026-05-28');
    expect(eveningLocal.toISOString().slice(0, 10)).toBe('2026-05-29');
  });
});

describe('localDateIsoFromTimestamp', () => {
  it('formats the local calendar date from an ISO timestamp', () => {
    expect(localDateIsoFromTimestamp('2026-05-07T17:47:00Z')).toBe(
      localDateIso(new Date('2026-05-07T17:47:00Z')),
    );
  });
});
