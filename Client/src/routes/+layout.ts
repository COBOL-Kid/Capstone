import { ensureAppBootstrapped } from '$lib/api/bootstrap';
import '../app.css';

export const prerender = true;

export async function load() {
  if (typeof window !== 'undefined') {
    await ensureAppBootstrapped();
  }
  return { bootstrapped: typeof window !== 'undefined' };
}
