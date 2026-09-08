import { ensureCsrfToken } from '$lib/api/client';
import { authStore } from '$lib/stores/auth.svelte';

let bootstrapPromise: Promise<void> | null = null;
let bootstrapped = false;

export function isAppBootstrapped(): boolean {
  return bootstrapped;
}

/** Issues the CSRF cookie then hydrates the session. */
export async function runAppBootstrap(): Promise<void> {
  try {
    await ensureCsrfToken();
  } catch {
    // Bootstrap continues without CSRF; mutating requests will fail naturally.
  }
  try {
    await authStore.validateSession();
  } catch {
    // Cold visits without a session simply render signed-out.
  }
}

/** Ensures bootstrap has run exactly once. Safe to await concurrently across layouts and guarded routes. */
export function ensureAppBootstrapped(): Promise<void> {
  if (typeof window === 'undefined') {
    return Promise.resolve();
  }
  if (!bootstrapPromise) {
    bootstrapPromise = runAppBootstrap().finally(() => {
      bootstrapped = true;
    });
  }
  return bootstrapPromise;
}

/** Resets bootstrap state for test isolation. */
export function resetAppBootstrap(): void {
  bootstrapPromise = null;
  bootstrapped = false;
}
