import { apiConfig, isSameBackendOrigin } from './config';

/** HTTP error with parsed body. Thrown by apiFetch for non-2xx responses. */
export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(status: number, body: unknown) {
    super(typeof body === 'string' && body ? body : `Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

type UnauthorizedHandler = (url: string) => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;

/** Registered once by the auth store; mirrors Angular's unauthorizedInterceptor. */
export function onUnauthorized(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler;
}

export function readXsrfToken(): string | null {
  if (typeof document === 'undefined') {
    return null;
  }
  for (const part of document.cookie.split(';')) {
    const [name, ...rest] = part.trim().split('=');
    if (name === 'XSRF-TOKEN') {
      return decodeURIComponent(rest.join('='));
    }
  }
  return null;
}

const MUTATING = new Set(['POST', 'PATCH', 'PUT', 'DELETE']);

let csrfPromise: Promise<void> | null = null;

/** Issues or ensures the CSRF token cookie is present before mutating requests. */
export async function ensureCsrfToken(): Promise<void> {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return;
  }
  if (readXsrfToken()) {
    return;
  }
  if (!csrfPromise) {
    csrfPromise = (async () => {
      try {
        await apiGet<unknown>(`${apiConfig.authUrl}/csrf`);
      } catch {
        // Bootstrap continues without CSRF; mutating requests will fail naturally.
      } finally {
        csrfPromise = null;
      }
    })();
  }
  return csrfPromise;
}

/** Resets in-flight CSRF request promise for test isolation. */
export function resetCsrfToken(): void {
  csrfPromise = null;
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export async function apiFetch<T>(url: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);

  if (MUTATING.has(method) && isSameBackendOrigin(url)) {
    let token = readXsrfToken();
    if (!token && typeof window !== 'undefined' && typeof document !== 'undefined') {
      await ensureCsrfToken();
      token = readXsrfToken();
    }
    if (token && !headers.has('X-XSRF-TOKEN')) {
      headers.set('X-XSRF-TOKEN', token);
    }
    if (!headers.has('Content-Type') && init.body !== undefined) {
      headers.set('Content-Type', 'application/json');
    }
  }

  const response = await fetch(url, { ...init, method, headers, credentials: 'include' });

  if (response.status === 401 && isSameBackendOrigin(url)) {
    if (
      !url.includes('/api/auth/authenticate') &&
      !url.includes('/api/auth/register') &&
      !url.includes('/api/auth/csrf')
    ) {
      unauthorizedHandler?.(url);
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, await parseBody(response));
  }

  return (await parseBody(response)) as T;
}

export function apiGet<T>(url: string, init: RequestInit = {}): Promise<T> {
  return apiFetch<T>(url, { ...init, method: 'GET' });
}

export function apiPost<T>(url: string, body?: unknown, init: RequestInit = {}): Promise<T> {
  return apiFetch<T>(url, {
    ...init,
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export function apiPatch<T>(url: string, body?: unknown, init: RequestInit = {}): Promise<T> {
  return apiFetch<T>(url, {
    ...init,
    method: 'PATCH',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export function apiDelete<T>(url: string, init: RequestInit = {}): Promise<T> {
  return apiFetch<T>(url, { ...init, method: 'DELETE' });
}
