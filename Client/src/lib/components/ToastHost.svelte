<script lang="ts">
  import { toastStore, type Toast } from '$lib/stores/toast.svelte';
  import { onMount } from 'svelte';

  const EXIT_ANIMATION_MS = 200;

  let leaving = $state<ReadonlySet<number>>(new Set());
  const timers = new Map<
    number,
    { handle: ReturnType<typeof setTimeout> | null; remaining: number; startedAt: number }
  >();
  let timersPaused = $state(false);
  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function dismiss(id: number): void {
    if (leaving.has(id)) {
      return;
    }
    clearTimer(id);
    leaving = new Set(leaving).add(id);
    if (prefersReducedMotion) {
      remove(id);
      return;
    }
    setTimeout(() => remove(id), EXIT_ANIMATION_MS);
  }

  function remove(id: number): void {
    toastStore.dismiss(id);
    leaving = new Set([...leaving].filter((entry) => entry !== id));
  }

  function startTimer(id: number, durationMs: number): void {
    if (timersPaused) {
      timers.set(id, { handle: null, remaining: durationMs, startedAt: Date.now() });
      return;
    }
    timers.set(id, {
      handle: setTimeout(() => dismiss(id), durationMs),
      remaining: durationMs,
      startedAt: Date.now(),
    });
  }

  function clearTimer(id: number): void {
    const state = timers.get(id);
    if (state?.handle != null) {
      clearTimeout(state.handle);
    }
    timers.delete(id);
  }

  function pauseTimers(): void {
    if (timersPaused) {
      return;
    }
    timersPaused = true;
    for (const state of timers.values()) {
      if (state.handle === null) {
        continue;
      }
      clearTimeout(state.handle);
      state.handle = null;
      state.remaining = Math.max(0, state.remaining - (Date.now() - state.startedAt));
    }
  }

  function resumeTimers(): void {
    if (!timersPaused) {
      return;
    }
    timersPaused = false;
    for (const [id, state] of timers) {
      if (state.handle !== null) {
        continue;
      }
      if (state.remaining <= 0) {
        dismiss(id);
        continue;
      }
      state.startedAt = Date.now();
      state.handle = setTimeout(() => dismiss(id), state.remaining);
    }
  }

  function role(toast: Toast): 'alert' | 'status' {
    return toast.variant === 'error' ? 'alert' : 'status';
  }

  $effect(() => {
    const current = toastStore.toasts;
    const currentIds = new Set(current.map((toast) => toast.id));
    for (const toast of current) {
      if (!timers.has(toast.id) && !leaving.has(toast.id) && toast.durationMs > 0) {
        startTimer(toast.id, toast.durationMs);
      }
    }
    for (const id of [...timers.keys()]) {
      if (!currentIds.has(id)) {
        clearTimer(id);
      }
    }
  });

  onMount(() => {
    return () => {
      for (const id of [...timers.keys()]) {
        clearTimer(id);
      }
    };
  });
</script>

{#if toastStore.toasts.length > 0}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div
    aria-live="polite"
    class="hc-toast-host"
    onmouseenter={pauseTimers}
    onmouseleave={resumeTimers}
  >
    {#each toastStore.toasts as toast (toast.id)}
      <div
        class="hc-toast"
        class:hc-toast--error={toast.variant === 'error'}
        class:hc-toast--info={toast.variant === 'info'}
        class:hc-toast--leaving={leaving.has(toast.id)}
        class:hc-toast--success={toast.variant === 'success'}
        role={role(toast)}
      >
        <span aria-hidden="true" class="hc-toast__icon">
          {#if toast.variant === 'success'}
            <svg fill="none" height="18" viewBox="0 0 24 24" width="18">
              <path
                d="M20 6 9 17l-5-5"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2.25"
              />
            </svg>
          {:else if toast.variant === 'error'}
            <svg fill="none" height="18" viewBox="0 0 24 24" width="18">
              <path
                d="M12 8v5m0 3.5h.01M10.3 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.7 3.86a2 2 0 0 0-3.4 0Z"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
              />
            </svg>
          {:else}
            <svg fill="none" height="18" viewBox="0 0 24 24" width="18">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" />
              <path
                d="M12 11v5m0-8.5h.01"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-width="2"
              />
            </svg>
          {/if}
        </span>
        <p class="hc-toast__message">{toast.message}</p>
        <button
          aria-label="Dismiss notification"
          class="hc-toast__close"
          onclick={() => dismiss(toast.id)}
          type="button"
        >
          ×
        </button>
      </div>
    {/each}
  </div>
{/if}

<style>
  .hc-toast-host {
    position: fixed;
    bottom: clamp(1rem, 4vw, 1.75rem);
    right: clamp(1rem, 4vw, 1.75rem);
    z-index: 70;
    display: flex;
    flex-direction: column;
    gap: var(--space-sm);
    width: min(24rem, calc(100vw - 2rem));
    pointer-events: none;
  }

  .hc-toast {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    padding: 0.85rem 0.95rem;
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border-strong);
    background: var(--color-surface);
    box-shadow: var(--shadow-lg);
    color: var(--color-text-on-dark);
    pointer-events: auto;
    animation: hc-toast-enter var(--dur-base) var(--ease-emphasized);
  }

  .hc-toast--leaving {
    animation: hc-toast-exit var(--dur-base) var(--ease-standard) forwards;
  }

  .hc-toast__icon {
    display: grid;
    place-items: center;
    flex-shrink: 0;
    width: 1.75rem;
    height: 1.75rem;
    border-radius: var(--radius-pill);
  }

  .hc-toast--success .hc-toast__icon {
    color: var(--color-alert-success-text);
    background: var(--color-alert-success-bg);
  }

  .hc-toast--error .hc-toast__icon {
    color: var(--color-alert-error-text);
    background: var(--color-alert-error-bg);
  }

  .hc-toast--info .hc-toast__icon {
    color: var(--color-blue-200);
    background: var(--color-accent-tint);
  }

  .hc-toast__message {
    flex: 1;
    margin: 0;
    font-size: var(--text-sm);
    line-height: 1.4;
    overflow-wrap: anywhere;
  }

  .hc-toast__close {
    flex-shrink: 0;
    display: grid;
    place-items: center;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: var(--radius-pill);
    color: var(--color-text-muted);
    font-size: 1.25rem;
    line-height: 1;
    cursor: pointer;
    border: none;
    background: transparent;
    transition:
      background-color var(--dur-fast) var(--ease-standard),
      color var(--dur-fast) var(--ease-standard);
  }

  .hc-toast__close:hover {
    background: rgba(245, 249, 255, 0.12);
    color: var(--color-text-on-dark);
  }

  @keyframes hc-toast-enter {
    from {
      opacity: 0;
      transform: translateY(0.75rem) scale(0.96);
    }
  }

  @keyframes hc-toast-exit {
    to {
      opacity: 0;
      transform: translateX(0.75rem) scale(0.98);
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .hc-toast,
    .hc-toast--leaving {
      animation: none;
    }
  }
</style>
