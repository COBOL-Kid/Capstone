import { redirect } from '@sveltejs/kit';
import { authStore } from '$lib/stores/auth.svelte';

export const prerender = false;
export const ssr = false;

export async function load() {
  const ok = await authStore.validateSession();
  if (!ok) {
    redirect(302, '/sign-in');
  }
  return {};
}
