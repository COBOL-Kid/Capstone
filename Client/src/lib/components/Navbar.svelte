<script lang="ts">
  import { authStore } from '$lib/stores/auth.svelte';
  import { page } from '$app/stores';

  import AccountDrawer from './AccountDrawer.svelte';

  interface AccountDrawerApi {
    show: () => void;
  }

  let drawerOpen = $state(false);
  let accountDrawer = $state<AccountDrawerApi | undefined>(undefined);
  let scrolled = $state(false);

  const homeLink = $derived(authStore.isSignedIn ? '/home' : '/');

  function openAccountDrawer(): void {
    accountDrawer?.show();
  }

  function handleScroll(): void {
    scrolled = window.scrollY > 8;
  }
</script>

<svelte:window onscroll={handleScroll} />

<div class="navbar-host">
  <nav aria-label="Primary" class="navbar" class:navbar--scrolled={scrolled}>
    <a class="navbar__brand" href={homeLink}>
      <span aria-hidden="true" class="navbar__brand-mark">
        <svg fill="none" height="20" viewBox="0 0 24 24" width="20">
          <path
            d="M4 16.5 5.4 10a3 3 0 0 1 2.9-2.3h7.4a3 3 0 0 1 2.9 2.3L20 16.5M4 16.5h16M4 16.5v2.3a.7.7 0 0 1-.7.7H3a1 1 0 0 1-1-1v-2h2Zm16 0v2.3a.7.7 0 0 0 .7.7h.3a1 1 0 0 0 1-1v-2h-2Z"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.6"
          />
          <circle cx="6.8" cy="16.5" r="1.1" fill="currentColor" />
          <circle cx="17.2" cy="16.5" r="1.1" fill="currentColor" />
        </svg>
      </span>
      <span class="navbar__brand-text">Honest Car</span>
    </a>

    <ul class="navbar__list">
      <li class="navbar__item">
        <a
          aria-current={$page.url.pathname === homeLink ? 'page' : undefined}
          class="navbar__link"
          class:navbar__link--active={$page.url.pathname === homeLink}
          href={homeLink}
        >
          Home
        </a>
      </li>
      <li class="navbar__item">
        <a
          aria-current={$page.url.pathname === '/our-services' ? 'page' : undefined}
          class="navbar__link"
          class:navbar__link--active={$page.url.pathname === '/our-services'}
          href="/our-services"
        >
          Our Services
        </a>
      </li>
    </ul>

    <button class="navbar__account-button" onclick={openAccountDrawer} type="button">
      <span aria-hidden="true" class="navbar__account-avatar">
        <svg fill="none" height="16" viewBox="0 0 24 24" width="16">
          <circle cx="12" cy="8.5" r="3.5" stroke="currentColor" stroke-width="1.7" />
          <path
            d="M5 19.5a7 7 0 0 1 14 0"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-width="1.7"
          />
        </svg>
      </span>
      <span class="navbar__account-label">Account</span>
    </button>
  </nav>

  <AccountDrawer bind:this={accountDrawer} bind:open={drawerOpen} />
</div>

<style>
  .navbar-host {
    display: block;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .navbar {
    height: var(--navbar-height);
    background-color: var(--color-surface);
    border-bottom: 1px solid var(--color-blue-800);
    box-shadow: var(--shadow-sm);
    display: flex;
    align-items: center;
    gap: clamp(1rem, 3vw, 2rem);
    padding: 0 clamp(1rem, 4vw, 2.5rem);
    transition:
      box-shadow var(--dur-base) var(--ease-standard),
      border-color var(--dur-base) var(--ease-standard),
      background-color var(--dur-base) var(--ease-standard);
  }

  .navbar--scrolled {
    box-shadow: var(--shadow-md);
    border-bottom-color: var(--color-blue-700);
  }

  .navbar__brand {
    display: inline-flex;
    align-items: center;
    gap: 0.6rem;
    color: var(--color-text-on-dark);
    font-weight: 700;
    letter-spacing: 0.04em;
    transition: color var(--dur-fast) var(--ease-standard);
  }

  .navbar__brand:hover {
    color: var(--color-blue-100);
  }

  .navbar__brand-mark {
    display: grid;
    place-items: center;
    width: 2.1rem;
    height: 2.1rem;
    border-radius: var(--radius-pill);
    color: var(--color-blue-100);
    background: var(--color-accent-tint);
    border: 1px solid var(--color-border-subtle);
  }

  .navbar__brand-text {
    font-size: 1.05rem;
  }

  @media (max-width: 420px) {
    .navbar__brand-text {
      display: none;
    }
  }

  .navbar__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    gap: clamp(1rem, 3vw, 2rem);
    align-items: center;
  }

  .navbar__account-button {
    margin-left: auto;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    border: 1px solid var(--color-blue-600);
    border-radius: var(--radius-pill);
    background: transparent;
    color: var(--color-text-on-dark);
    padding: 0.45rem 1rem 0.45rem 0.5rem;
    font-size: 0.875rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    cursor: pointer;
    transition:
      border-color var(--dur-fast) var(--ease-standard),
      color var(--dur-fast) var(--ease-standard),
      background-color var(--dur-fast) var(--ease-standard);
  }

  .navbar__account-button:hover {
    border-color: var(--color-blue-300);
    background-color: rgba(122, 162, 247, 0.12);
    color: var(--color-blue-100);
  }

  .navbar__account-avatar {
    display: grid;
    place-items: center;
    width: 1.75rem;
    height: 1.75rem;
    border-radius: var(--radius-pill);
    background: var(--color-accent-tint);
    color: var(--color-blue-100);
  }

  @media (max-width: 420px) {
    .navbar__account-label {
      display: none;
    }

    .navbar__account-button {
      padding: 0.4rem;
    }
  }

  .navbar__item {
    display: flex;
  }

  .navbar__link {
    position: relative;
    display: inline-flex;
    align-items: center;
    height: var(--navbar-height);
    padding: 0 0.25rem;
    color: var(--color-text-muted);
    font-size: 1rem;
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    transition: color var(--dur-fast) var(--ease-standard);
  }

  .navbar__link::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 1rem;
    height: 2px;
    border-radius: var(--radius-pill);
    background-color: var(--color-accent);
    transform: scaleX(0);
    transform-origin: left center;
    transition: transform var(--dur-base) var(--ease-standard);
  }

  .navbar__link:hover {
    color: var(--color-blue-100);
  }

  .navbar__link:hover::after {
    transform: scaleX(1);
  }

  .navbar__link--active {
    color: var(--color-blue-100);
  }

  .navbar__link--active::after {
    transform: scaleX(1);
  }

  @media (prefers-reduced-motion: reduce) {
    .navbar,
    .navbar__link,
    .navbar__link::after,
    .navbar__brand,
    .navbar__account-button {
      transition: none;
    }
  }
</style>
