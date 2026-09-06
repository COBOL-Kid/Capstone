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

<nav aria-label="Primary" class="border-b border-border-subtle bg-navy-950/80 backdrop-blur">
  <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
    <a href="/" class="font-display text-lg font-extrabold tracking-tight text-ink">Honest Car</a>
    <div class="flex items-center gap-5 text-sm font-semibold text-muted">
      <a href="/our-services" class="transition-colors hover:text-ink">Our Services</a>
      <a href="/sign-in" class="transition-colors hover:text-ink">Sign In</a>
      <a
        href="/sign-up"
        class="rounded-full bg-accent px-4 py-2 text-ink shadow-glow transition-transform hover:-translate-y-px"
      >
        Sign Up
      </a>
    </div>
  </div>
</nav>

<main>
  {@render children()}
</main>
