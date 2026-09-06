<script lang="ts">
  import { ApiError } from '$lib/api/client';
  import { localDateIsoFromTimestamp } from '$lib/utils/local-date';
  import { authStore } from '$lib/stores/auth.svelte';
  import { toastStore } from '$lib/stores/toast.svelte';
  import { goto } from '$app/navigation';
  import type {
    AccountChangeType,
    AccountDetails,
    AuthErrorMessage,
    AuthModalMode,
  } from '$lib/models/auth';

  import AuthModal from './AuthModal.svelte';
  import ChangeEmailModal from './ChangeEmailModal.svelte';
  import ChangePasswordModal from './ChangePasswordModal.svelte';
  import ChangeSmsModal from './ChangeSmsModal.svelte';
  import ServerError from './ServerError.svelte';

  type Status = 'signed-out' | 'loading' | 'signed-in' | 'unauthorized' | 'error';

  interface Props {
    open?: boolean;
  }

  let { open = $bindable(false) }: Props = $props();

  let isClosing = $state(false);
  let status = $state<Status>('signed-out');
  let account = $state<AccountDetails | null>(null);
  let authModalMode = $state<AuthModalMode | null>(null);
  let activeChangeModal = $state<AccountChangeType | null>(null);
  let resumeChangeOnVerifyStep = $state(false);
  let isLoggingOut = $state(false);
  let registrationVerificationError = $state<AuthErrorMessage | null>(null);
  let isResendingRegistrationVerification = $state(false);
  let closeTimer: ReturnType<typeof setTimeout> | null = null;

  const CLOSE_DELAY_MS = 240;

  export function show(): void {
    if (closeTimer !== null) {
      clearTimeout(closeTimer);
      closeTimer = null;
    }
    isClosing = false;
    open = true;
    void loadAccountDetails();
  }

  function close(): void {
    if (!open || isClosing) {
      return;
    }
    activeChangeModal = null;
    resumeChangeOnVerifyStep = false;
    isClosing = true;
    closeTimer = setTimeout(() => {
      open = false;
      isClosing = false;
      closeTimer = null;
    }, CLOSE_DELAY_MS);
  }

  function handleKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Escape') {
      return;
    }
    if (!open || activeChangeModal || authModalMode) {
      return;
    }
    close();
  }

  async function loadAccountDetails(forceRefresh = false): Promise<void> {
    const cached = forceRefresh ? null : authStore.account;
    if (cached) {
      setSignedInAccount(cached);
      await checkPendingAccountChange();
      return;
    }
    account = null;
    if (!authStore.isSignedIn) {
      resetSignedInState();
      status = 'signed-out';
      return;
    }
    status = 'loading';
    try {
      const details = await authStore.getCurrentAccount({ forceRefresh });
      setSignedInAccount(details);
      await checkPendingAccountChange();
    } catch (error) {
      account = null;
      resetSignedInState();
      if (error instanceof ApiError && error.status === 401) {
        authStore.clearSession();
        status = 'unauthorized';
        return;
      }
      if (error instanceof TypeError) {
        authStore.clearSession();
        status = 'signed-out';
        return;
      }
      status = 'error';
    }
  }

  async function checkPendingAccountChange(): Promise<void> {
    try {
      const pending = await authStore.getPendingAccountChange();
      if (!pending || activeChangeModal) {
        return;
      }
      resumeChangeOnVerifyStep = true;
      activeChangeModal = pending.changeType;
    } catch {
      // Pending-change lookup is best-effort; the drawer works without it.
    }
  }

  async function refreshAccountAfterChange(): Promise<void> {
    try {
      account = await authStore.getCurrentAccount({ forceRefresh: true });
    } catch {
      // Keep the previously loaded account on refresh failure.
    }
  }

  function setSignedInAccount(details: AccountDetails): void {
    account = details;
    registrationVerificationError = null;
    status = 'signed-in';
  }

  function resetSignedInState(): void {
    activeChangeModal = null;
    resumeChangeOnVerifyStep = false;
    registrationVerificationError = null;
    account = null;
  }

  function openChangeModal(changeType: AccountChangeType): void {
    resumeChangeOnVerifyStep = false;
    activeChangeModal = changeType;
  }

  function closeChangeModal(): void {
    activeChangeModal = null;
    resumeChangeOnVerifyStep = false;
  }

  function onEmailChanged(): void {
    toastStore.success('Email updated.');
    void refreshAccountAfterChange();
  }

  function onSmsChanged(): void {
    toastStore.success('SMS number updated.');
    void refreshAccountAfterChange();
  }

  async function onPasswordChanged(): Promise<void> {
    toastStore.success('Password updated. Please sign in with your new password.');
    activeChangeModal = null;
    resumeChangeOnVerifyStep = false;
    close();
    await goto('/sign-in');
  }

  async function logout(): Promise<void> {
    if (isLoggingOut) {
      return;
    }
    isLoggingOut = true;
    try {
      await authStore.logout();
    } catch {
      // Log out locally even when the server call fails.
    } finally {
      isLoggingOut = false;
    }
    authStore.clearSession();
    resetSignedInState();
    status = 'signed-out';
    await goto('/');
    close();
  }

  async function resendRegistrationVerification(): Promise<void> {
    if (isResendingRegistrationVerification) {
      return;
    }
    registrationVerificationError = null;
    isResendingRegistrationVerification = true;
    try {
      await authStore.resendEmailVerification();
    } catch (error) {
      registrationVerificationError = error as AuthErrorMessage;
    } finally {
      isResendingRegistrationVerification = false;
    }
  }

  function openAuthModal(mode: AuthModalMode): void {
    authModalMode = mode;
  }

  async function closeAuthModal(): Promise<void> {
    authModalMode = null;
    if (!open) {
      return;
    }
    if (authStore.isSignedIn) {
      await goto('/home');
      await loadAccountDetails(true);
      return;
    }
    resetSignedInState();
    status = 'signed-out';
  }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
  <div
    aria-hidden="true"
    class="hc-overlay-backdrop hc-overlay-backdrop--light"
    class:hc-overlay-backdrop--closing={isClosing}
    onclick={close}
  ></div>
{/if}

<div
  aria-hidden={!open}
  aria-labelledby="account-drawer-title"
  class="account-drawer"
  class:account-drawer--closing={isClosing}
  class:account-drawer--open={open}
  role="dialog"
  tabindex="-1"
>
  <header class="account-drawer__header">
    <div>
      <p class="hc-eyebrow hc-eyebrow--drawer">Honest Car</p>
      <h2 id="account-drawer-title">Account</h2>
    </div>
    <button
      aria-label="Close account panel"
      class="account-drawer__close"
      onclick={close}
      type="button"
    >
      ×
    </button>
  </header>

  <section aria-live="polite" class="account-drawer__content">
    {#if status === 'loading'}
      <p class="account-drawer__message" role="status">Loading account details…</p>
    {:else if status === 'signed-in' && account}
      <div class="account-drawer__profile">
        <p class="hc-label">Signed in as</p>
        <p class="account-drawer__name">
          {account.firstName}
          {account.lastName}
        </p>
        <a class="account-drawer__email" href="mailto:{account.email}">{account.email}</a>
      </div>

      {#if !account.emailVerified}
        <div class="hc-alert hc-alert--warning account-drawer__verification-warning" role="status">
          <p>Verify your email to add vehicles.</p>
          {#if registrationVerificationError}
            <ServerError error={registrationVerificationError} />
          {/if}
          <button
            class="hc-btn hc-btn--secondary"
            disabled={isResendingRegistrationVerification}
            onclick={resendRegistrationVerification}
            type="button"
          >
            {isResendingRegistrationVerification ? 'Sending code…' : 'Resend verification code'}
          </button>
        </div>
      {/if}

      <button class="hc-btn hc-btn--logout" disabled={isLoggingOut} onclick={logout} type="button">
        {isLoggingOut ? 'Logging out…' : 'Log out'}
      </button>

      <dl class="account-drawer__details">
        <div>
          <dt>SMS</dt>
          <dd>{account.userSms || 'Not provided'}</dd>
        </div>
        <div>
          <dt>Member Since</dt>
          <dd>{localDateIsoFromTimestamp(account.createdAt)}</dd>
        </div>
        <div>
          <dt>Last Updated</dt>
          <dd>{localDateIsoFromTimestamp(account.updatedAt)}</dd>
        </div>
      </dl>

      <div class="account-drawer__manage">
        <p class="hc-label">Manage account</p>
        <div class="account-drawer__actions">
          <button
            class="hc-btn hc-btn--secondary"
            onclick={() => openChangeModal('EMAIL')}
            type="button"
          >
            Change Email
          </button>
          <button
            class="hc-btn hc-btn--secondary"
            onclick={() => openChangeModal('SMS')}
            type="button"
          >
            Change SMS
          </button>
          <button
            class="hc-btn hc-btn--secondary"
            onclick={() => openChangeModal('PASSWORD')}
            type="button"
          >
            Change Password
          </button>
        </div>
      </div>
    {:else if status === 'unauthorized'}
      <p class="account-drawer__message" role="alert">
        Your session has expired. Sign in again to view your account details.
      </p>
      <div class="account-drawer__actions">
        <button
          class="hc-btn hc-btn--primary-inverse"
          onclick={() => openAuthModal('sign-in')}
          type="button"
        >
          Sign In
        </button>
        <button
          class="hc-btn hc-btn--secondary"
          onclick={() => openAuthModal('sign-up')}
          type="button"
        >
          Sign Up
        </button>
      </div>
    {:else if status === 'error'}
      <p class="account-drawer__message" role="alert">
        Unable to load account details. Please try again.
      </p>
    {:else}
      <p class="account-drawer__message">Sign in to view your account details.</p>
      <div class="account-drawer__actions">
        <button
          class="hc-btn hc-btn--primary-inverse"
          onclick={() => openAuthModal('sign-in')}
          type="button"
        >
          Sign In
        </button>
        <button
          class="hc-btn hc-btn--secondary"
          onclick={() => openAuthModal('sign-up')}
          type="button"
        >
          Sign Up
        </button>
      </div>
    {/if}
  </section>
</div>

{#if authModalMode}
  <AuthModal
    mode={authModalMode}
    onclose={closeAuthModal}
    onmodechange={(mode) => (authModalMode = mode)}
    useRoutingLinks={false}
  />
{/if}

{#if activeChangeModal === 'EMAIL' && account}
  <ChangeEmailModal
    currentEmail={account.email}
    onchanged={onEmailChanged}
    onclose={closeChangeModal}
    startOnVerifyStep={resumeChangeOnVerifyStep}
  />
{/if}

{#if activeChangeModal === 'SMS' && account}
  <ChangeSmsModal
    currentEmail={account.email}
    currentSms={account.userSms ?? ''}
    onchanged={onSmsChanged}
    onclose={closeChangeModal}
    startOnVerifyStep={resumeChangeOnVerifyStep}
  />
{/if}

{#if activeChangeModal === 'PASSWORD' && account}
  <ChangePasswordModal
    currentEmail={account.email}
    onchanged={onPasswordChanged}
    onclose={closeChangeModal}
    startOnVerifyStep={resumeChangeOnVerifyStep}
  />
{/if}

<style>
  .account-drawer {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 40;
    width: min(var(--hc-drawer-width), 100vw);
    padding: var(--hc-drawer-padding);
    background: var(--color-surface);
    border-left: 1px solid var(--color-blue-800);
    box-shadow: -12px 0 28px rgba(7, 20, 43, 0.45);
    color: var(--color-text-on-dark);
    transform: translateX(100%);
    visibility: hidden;
    display: flex;
    flex-direction: column;
    gap: var(--hc-drawer-section-gap);
    overflow-y: auto;
  }

  .account-drawer--open {
    transform: translateX(0);
    visibility: visible;
    animation: account-drawer-enter 240ms var(--ease-standard);
  }

  .account-drawer--closing {
    animation: account-drawer-exit 240ms var(--ease-standard) forwards;
  }

  @keyframes account-drawer-enter {
    from {
      opacity: 0;
      transform: translateX(1.5rem) scale(0.98);
    }

    to {
      opacity: 1;
      transform: translateX(0) scale(1);
    }
  }

  @keyframes account-drawer-exit {
    to {
      opacity: 0;
      transform: translateX(1.5rem) scale(0.98);
    }
  }

  .account-drawer__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.75rem;
  }

  .account-drawer h2 {
    margin: 0;
    font-size: var(--hc-dialog-title-size);
  }

  .account-drawer__close {
    width: var(--hc-drawer-close-size);
    height: var(--hc-drawer-close-size);
    min-width: var(--hc-drawer-close-size);
    min-height: var(--hc-drawer-close-size);
    border: 1px solid var(--color-blue-700);
    border-radius: 999px;
    background: transparent;
    color: var(--color-text-on-dark);
    font-size: 1.35rem;
    line-height: 1;
    cursor: pointer;
  }

  .account-drawer__content {
    display: flex;
    flex-direction: column;
    gap: var(--hc-drawer-content-gap);
  }

  .account-drawer__message {
    margin: 0;
    color: var(--color-text-muted);
    line-height: 1.5;
    font-size: 0.9rem;
  }

  .account-drawer__profile {
    border: 1px solid var(--color-blue-800);
    border-left: 3px solid var(--color-accent);
    border-radius: var(--radius-md);
    padding: var(--hc-drawer-profile-padding);
    background: var(--color-accent-tint);
  }

  .account-drawer__manage {
    display: grid;
    gap: 0.5rem;
  }

  .account-drawer__manage .hc-label {
    margin: 0;
  }

  .account-drawer__name {
    margin: 0 0 0.35rem;
    font-size: var(--hc-drawer-name-size);
    font-weight: 700;
  }

  .account-drawer__email {
    color: var(--color-blue-100);
    font-size: 0.9rem;
    overflow-wrap: anywhere;
  }

  .account-drawer__details {
    display: grid;
    gap: 0.75rem;
    margin: 0;
  }

  .account-drawer__details div {
    display: grid;
    gap: 0.2rem;
  }

  .account-drawer__details dt {
    color: var(--color-blue-300);
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .account-drawer__details dd {
    margin: 0;
    color: var(--color-text-on-dark);
    font-size: 0.9rem;
    overflow-wrap: anywhere;
  }

  .account-drawer__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.6rem;
  }

  .account-drawer__verification-warning {
    display: grid;
    gap: 0.6rem;
  }

  .account-drawer__verification-warning p {
    margin: 0;
  }

  .hc-overlay-backdrop--light {
    z-index: 30;
    background: var(--color-overlay-backdrop-light);
    backdrop-filter: blur(4px);
  }

  .hc-overlay-backdrop--closing {
    animation: hc-overlay-exit 220ms var(--ease-standard) forwards;
  }

  @keyframes hc-overlay-exit {
    to {
      opacity: 0;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .account-drawer--open,
    .account-drawer--closing {
      animation: none;
    }
  }
</style>
