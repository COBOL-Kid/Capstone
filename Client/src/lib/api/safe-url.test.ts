import { describe, expect, it } from 'vitest';

import { isSafeHttpUrl } from './safe-url';

describe('isSafeHttpUrl', () => {
  it('accepts http and https URLs', () => {
    expect(isSafeHttpUrl('https://example.com/img.jpg')).toBe(true);
    expect(isSafeHttpUrl('http://example.com/img.jpg')).toBe(true);
  });

  it('rejects null, empty, javascript, and relative URLs', () => {
    expect(isSafeHttpUrl(null)).toBe(false);
    expect(isSafeHttpUrl(undefined)).toBe(false);
    expect(isSafeHttpUrl('')).toBe(false);
    expect(isSafeHttpUrl('javascript:alert(1)')).toBe(false);
    expect(isSafeHttpUrl('/relative/path.jpg')).toBe(false);
  });
});
