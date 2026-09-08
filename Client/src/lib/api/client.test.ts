import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  ApiError,
  apiDelete,
  apiFetch,
  apiGet,
  apiPatch,
  apiPost,
  ensureCsrfToken,
  onUnauthorized,
  readXsrfToken,
  resetCsrfToken,
} from './client';

describe('client apiFetch', () => {
  const originalFetch = globalThis.fetch;
  const originalDocument = globalThis.document;
  const originalWindow = globalThis.window;

  beforeEach(() => {
    resetCsrfToken();
    let cookieStore = '';
    globalThis.document = {
      get cookie() {
        return cookieStore;
      },
      set cookie(value: string) {
        cookieStore = value;
      },
    } as unknown as Document;

    globalThis.window = {
      location: {
        origin: 'http://localhost:4200',
      },
    } as unknown as Window & typeof globalThis;
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    globalThis.document = originalDocument;
    globalThis.window = originalWindow;
    vi.restoreAllMocks();
  });

  it('reads XSRF token from document cookie', () => {
    document.cookie = 'foo=bar; XSRF-TOKEN=test-token-123; other=val';
    expect(readXsrfToken()).toBe('test-token-123');
  });

  it('returns null when XSRF-TOKEN cookie is missing', () => {
    document.cookie = 'foo=bar';
    expect(readXsrfToken()).toBeNull();
  });

  it('attaches X-XSRF-TOKEN header to mutating requests when cookie exists', async () => {
    document.cookie = 'XSRF-TOKEN=my-xsrf-val';

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ success: true })),
    });
    globalThis.fetch = fetchMock;

    await apiPost('/api/test', { data: 123 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;
    expect(headers.get('X-XSRF-TOKEN')).toBe('my-xsrf-val');
    expect(headers.get('Content-Type')).toBe('application/json');
    expect(options.credentials).toBe('include');
  });

  it('does not attach X-XSRF-TOKEN to GET requests', async () => {
    document.cookie = 'XSRF-TOKEN=my-xsrf-val';

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ data: 'ok' })),
    });
    globalThis.fetch = fetchMock;

    await apiGet('/api/test');

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;
    expect(headers.get('X-XSRF-TOKEN')).toBeNull();
  });

  it('does not attach X-XSRF-TOKEN to external mutating requests', async () => {
    document.cookie = 'XSRF-TOKEN=my-xsrf-val';

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({})),
    });
    globalThis.fetch = fetchMock;

    await apiFetch('https://external-service.com/api/post', {
      method: 'POST',
      body: JSON.stringify({}),
    });

    const [, options] = fetchMock.mock.calls[0];
    const headers = options.headers as Headers;
    expect(headers.get('X-XSRF-TOKEN')).toBeNull();
  });

  it('fetches CSRF token before mutating requests when cookie is missing', async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/auth/csrf')) {
        document.cookie = 'XSRF-TOKEN=bootstrap-token';
        return Promise.resolve({
          ok: true,
          status: 200,
          text: () => Promise.resolve(JSON.stringify({ csrf: true })),
        });
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => Promise.resolve(JSON.stringify({ result: 'done' })),
      });
    });
    globalThis.fetch = fetchMock;

    await apiPost('/api/vin', { vin: '123' });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[0][0]).toContain('/api/auth/csrf');
    const [, secondOptions] = fetchMock.mock.calls[1];
    const headers = secondOptions.headers as Headers;
    expect(headers.get('X-XSRF-TOKEN')).toBe('bootstrap-token');
  });

  it('calls onUnauthorized handler on 401 for same-origin non-auth requests', async () => {
    const handler = vi.fn();
    onUnauthorized(handler);

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Unauthorized'),
    });

    await expect(apiGet('/api/account/me')).rejects.toThrow(ApiError);
    expect(handler).toHaveBeenCalledWith('/api/account/me');
  });

  it('does not call onUnauthorized on 401 for sign-in (/authenticate)', async () => {
    const handler = vi.fn();
    onUnauthorized(handler);

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Invalid credentials'),
    });

    await expect(
      apiPost('/api/auth/authenticate', { email: 'a@b.com', password: 'pw' }),
    ).rejects.toThrow(ApiError);
    expect(handler).not.toHaveBeenCalled();
  });

  it('does not call onUnauthorized on 401 for register (/register)', async () => {
    const handler = vi.fn();
    onUnauthorized(handler);

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Unauthorized'),
    });

    await expect(
      apiPost('/api/auth/register', { email: 'a@b.com', password: 'pw' }),
    ).rejects.toThrow(ApiError);
    expect(handler).not.toHaveBeenCalled();
  });

  it('returns null on 204 No Content', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      text: () => Promise.resolve(''),
    });

    const result = await apiDelete('/api/vin/123');
    expect(result).toBeNull();
  });

  it('throws ApiError with status and parsed JSON body on failures', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      text: () => Promise.resolve(JSON.stringify({ message: 'Vehicle not found' })),
    });

    await expect(apiGet('/api/vin/unknown')).rejects.toSatisfy((err: unknown) => {
      expect(err).toBeInstanceOf(ApiError);
      const apiErr = err as ApiError;
      expect(apiErr.status).toBe(404);
      expect(apiErr.body).toEqual({ message: 'Vehicle not found' });
      return true;
    });
  });

  it('supports apiPatch', async () => {
    document.cookie = 'XSRF-TOKEN=patch-token';
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ updated: true })),
    });
    globalThis.fetch = fetchMock;

    const res = await apiPatch<{ updated: boolean }>('/api/account/me', { firstName: 'Jane' });
    expect(res).toEqual({ updated: true });
    expect(fetchMock.mock.calls[0][1].method).toBe('PATCH');
  });
});
