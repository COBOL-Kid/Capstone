<script lang="ts">
  import { authStore } from '$lib/stores/auth.svelte';
  import { goto } from '$app/navigation';
  import type { AuthModalMode } from '$lib/models/auth';

  import AuthModal from '$lib/components/AuthModal.svelte';

  interface Props {
    mode?: AuthModalMode | null;
  }

  let { mode = null }: Props = $props();

  let isClosing = $state(false);
  let closeTimer: ReturnType<typeof setTimeout> | null = null;

  async function closeAuthModal(): Promise<void> {
    if (isClosing) {
      return;
    }
    isClosing = true;
    closeTimer = setTimeout(async () => {
      closeTimer = null;
      const destination = authStore.isSignedIn ? '/home' : '/';
      await goto(destination);
    }, 240);
  }
</script>

<section aria-labelledby="landing-title" class="landing hc-min-h-below-navbar">
  <img alt="" class="landing__image" fetchpriority="high" src="landing-background.png" />
  <div aria-hidden="true" class="landing__overlay"></div>

  <div class="landing__content hc-min-h-below-navbar">
    <div class="landing__intro">
      <h1 class="landing__title" id="landing-title">Honest Car</h1>
      <p class="landing__tagline">
        Everything you need to know about your car. Maintenance, recalls, and warranty coverage, all
        in one place.
      </p>
    </div>

    <div aria-label="Authentication actions" class="landing__actions" role="group">
      <a class="hc-btn hc-btn--primary" href="/sign-up">Sign Up</a>
      <a class="hc-btn hc-btn--ghost" href="/sign-in">Sign In</a>
    </div>
  </div>

  {#if mode}
    <div class:hc-modal-host--closing={isClosing}>
      <AuthModal {mode} onclose={closeAuthModal} useRoutingLinks={true} />
    </div>
  {/if}
</section>

<style>
  .landing {
    position: relative;
    width: 100%;
    overflow: hidden;
    background-color: var(--color-bg);
  }

  .landing__image {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
  }

  .landing__overlay {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(120% 80% at 50% 0%, rgba(19, 49, 92, 0.35) 0%, transparent 60%),
      linear-gradient(
        180deg,
        rgba(7, 20, 43, 0.5) 0%,
        rgba(7, 20, 43, 0.78) 55%,
        rgba(7, 20, 43, 0.94) 100%
      );
    pointer-events: none;
  }

  .landing__content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    padding: clamp(3rem, 12vh, 8rem) clamp(1rem, 4vw, 2.5rem) clamp(2rem, 8vh, 6rem);
    text-align: center;
  }

  .landing__intro {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: clamp(0.75rem, 2vh, 1.25rem);
    animation: landing-rise 600ms var(--ease-standard) both;
  }

  .landing__title {
    font-size: clamp(2.5rem, 7vw, 5rem);
    line-height: 1.05;
    letter-spacing: 0.01em;
    color: var(--color-text-on-dark);
    text-shadow:
      0 6px 24px rgba(7, 20, 43, 0.6),
      0 1px 2px rgba(7, 20, 43, 0.8);
    max-width: 20ch;
  }

  .landing__tagline {
    margin: 0;
    max-width: 46ch;
    font-size: clamp(1rem, 2.2vw, 1.3rem);
    line-height: 1.5;
    color: var(--color-blue-100);
    text-shadow: 0 2px 12px rgba(7, 20, 43, 0.7);
  }

  .landing__actions {
    margin-top: auto;
    display: flex;
    gap: clamp(0.75rem, 2vw, 1.25rem);
    flex-wrap: wrap;
    justify-content: center;
    animation: landing-rise 600ms var(--ease-standard) 140ms both;
  }

  @keyframes landing-rise {
    from {
      opacity: 0;
      transform: translateY(1.25rem);
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .landing__intro,
    .landing__actions {
      animation: none;
    }
  }

  @media (max-width: 480px) {
    .landing__actions {
      flex-direction: column;
      width: min(100%, 18rem);
    }

    .landing__actions :global(.hc-btn) {
      width: 100%;
    }
  }

  .hc-modal-host--closing :global(.hc-overlay-backdrop) {
    animation: hc-overlay-exit 220ms var(--ease-standard) forwards;
  }

  .hc-modal-host--closing :global(.hc-dialog) {
    animation: hc-dialog-exit 240ms var(--ease-standard) forwards;
  }

  @keyframes hc-overlay-exit {
    to {
      opacity: 0;
    }
  }

  @keyframes hc-dialog-exit {
    to {
      opacity: 0;
      transform: translateY(0.75rem) scale(0.98);
    }
  }
</style>
