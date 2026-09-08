<script lang="ts">
  import { ensureAppBootstrapped, isAppBootstrapped } from '$lib/api/bootstrap';
  import Navbar from '$lib/components/Navbar.svelte';
  import ToastHost from '$lib/components/ToastHost.svelte';
  import { onMount } from 'svelte';
  import type { Snippet } from 'svelte';

  interface Props {
    data?: { bootstrapped?: boolean };
    children: Snippet;
  }

  let { data, children }: Props = $props();
  let clientBootstrapped = $state(false);
  const bootstrapped = $derived(
    Boolean(data?.bootstrapped || isAppBootstrapped() || clientBootstrapped),
  );

  onMount(async () => {
    if (!bootstrapped) {
      await ensureAppBootstrapped();
      clientBootstrapped = true;
    }
  });
</script>

<svelte:head>
  <title>Honest Car</title>
</svelte:head>

{#if !bootstrapped}
  <div class="app-splash" role="status" aria-busy="true" aria-live="polite">
    <div class="app-splash__brand">
      <h1 class="app-splash__title">Honest Car</h1>
    </div>
    <div class="app-splash__spinner" aria-hidden="true"></div>
    <p class="app-splash__status">Loading…</p>
  </div>
{/if}

<div
  class="app-shell"
  inert={!bootstrapped ? true : undefined}
  aria-hidden={!bootstrapped ? 'true' : undefined}
>
  <Navbar />

  <main>
    {@render children()}
  </main>

  <ToastHost />
</div>
