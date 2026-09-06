import { apiGet } from '$lib/api/client';
import { apiConfig } from '$lib/api/config';
import { authStore } from '$lib/stores/auth.svelte';

/** Issues the CSRF cookie then hydrates the session. Runs once in +layout.svelte. */
export async function runAppBootstrap(): Promise<void> {
  try {
    await apiGet<unknown>(`${apiConfig.authUrl}/csrf`);
  } catch {
    // Bootstrap continues without CSRF; mutating requests will fail naturally.
  }
  try {
    await authStore.validateSession();
  } catch {
    // Cold visits without a session simply render signed-out.
  }
}
