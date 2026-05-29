import { describe, expect, it } from 'vitest';

import { localDateIso } from './local-date';

describe('localDateIso', () => {
  it('formats the local calendar date, not UTC', () => {
    const eveningLocal = new Date(2026, 4, 28, 22, 0, 0);

    expect(localDateIso(eveningLocal)).toBe('2026-05-28');
    expect(eveningLocal.toISOString().slice(0, 10)).toBe('2026-05-29');
  });
});
