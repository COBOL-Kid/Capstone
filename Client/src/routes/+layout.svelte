<script lang="ts">
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();
</script>

<svelte:head>
  <title>Honest Car</title>
</svelte:head>

<div class="app-splash" role="status" aria-busy="true" aria-live="polite" hidden>
  <div class="app-splash__brand">
    <h1 class="app-splash__title">Honest Car</h1>
  </div>
  <p class="app-splash__status">Loading…</p>
</div>

<nav aria-label="Primary" class="border-b border-white/10">
  <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
    <a href="/" class="font-bold tracking-tight">Honest Car</a>
    <div class="flex gap-4 text-sm">
      <a href="/our-services">Our Services</a>
      <a href="/sign-in">Sign In</a>
      <a href="/sign-up">Sign Up</a>
    </div>
  </div>
</nav>

<main>
  {@render children()}
</main>
