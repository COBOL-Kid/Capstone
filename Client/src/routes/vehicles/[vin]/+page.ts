import { redirect } from '@sveltejs/kit';
import { ensureAppBootstrapped } from '$lib/api/bootstrap';
import { authStore } from '$lib/stores/auth.svelte';

export const prerender = false;
export const ssr = false;

export async function load() {
  await ensureAppBootstrapped();
  const ok = await authStore.validateSession();
  if (!ok) {
    redirect(302, '/sign-in');
  }
  return {};
}
