<script lang="ts">
  import { runAppBootstrap } from '$lib/api/bootstrap';
  import Navbar from '$lib/components/Navbar.svelte';
  import ToastHost from '$lib/components/ToastHost.svelte';
  import { onMount } from 'svelte';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();
  let bootstrapped = $state(false);

  onMount(async () => {
    await runAppBootstrap();
    bootstrapped = true;
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

<Navbar />

<main>
  {@render children()}
</main>

<ToastHost />
