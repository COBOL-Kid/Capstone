<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import type { AuthErrorMessage } from '$lib/models/auth';

  import Dialog from './Dialog.svelte';
  import EmailVerificationStep from './EmailVerificationStep.svelte';
  import ServerError from './ServerError.svelte';

  const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;

  type Step = 'details' | 'verify';

  interface Props {
    currentEmail: string;
    startOnVerifyStep?: boolean;
    onclose: () => void;
    onchanged: () => void;
  }

  let { currentEmail, startOnVerifyStep = false, onclose, onchanged }: Props = $props();

  let step = $state<Step>('details');
  let isSubmitting = $state(false);
  let isVerifying = $state(false);
  let isResending = $state(false);
  let serverError = $state<AuthErrorMessage | null>(null);
  let verificationError = $state<AuthErrorMessage | null>(null);

  let currentPassword = $state('');
  let newPassword = $state('');
  let confirmPassword = $state('');
  let touched = $state(false);

  const busy = $derived(isSubmitting || isVerifying || isResending);
  const passwordsMatch = $derived(newPassword === confirmPassword);

  onMount(() => {
    if (startOnVerifyStep) {
      step = 'verify';
    }
  });

  async function submitDetails(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }
    serverError = null;
    touched = true;
    const valid =
      currentPassword.length > 0 &&
      newPassword.length >= 8 &&
      newPassword.length <= 72 &&
      passwordPattern.test(newPassword) &&
      confirmPassword.length > 0 &&
      passwordsMatch;
    if (!valid) {
      return;
    }
    isSubmitting = true;
    try {
      await authStore.initiateAccountChange({
        changeType: 'PASSWORD',
        currentPassword,
        newPassword,
      });
      verificationError = null;
      step = 'verify';
    } catch (error) {
      serverError = error as AuthErrorMessage;
    } finally {
      isSubmitting = false;
    }
  }

  async function verifyCode(code: string): Promise<void> {
    if (isVerifying || isResending) {
      return;
    }
    verificationError = null;
    isVerifying = true;
    try {
      await authStore.verifyAccountChange(code);
      onchanged();
      onclose();
    } catch (error) {
      verificationError = error as AuthErrorMessage;
    } finally {
      isVerifying = false;
    }
  }

  async function resendCode(): Promise<void> {
    if (isResending || isVerifying) {
      return;
    }
    verificationError = null;
    isResending = true;
    try {
      await authStore.resendAccountChangeCode();
    } catch (error) {
      verificationError = error as AuthErrorMessage;
    } finally {
      isResending = false;
    }
  }
</script>

<Dialog
  {busy}
  closeLabel="Close change password dialog"
  labelledBy="change-password-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <p class="hc-eyebrow">Honest Car</p>
      <h2 id="change-password-modal-title">Change Password</h2>
    </header>
  {/snippet}

  {#snippet children()}
    {#if step === 'details'}
      <div class="hc-callout" role="note">
        <p class="hc-callout-title">Password requirements</p>
        <ul>
          <li>At least 8 characters, at most 72</li>
          <li>Uppercase, lowercase, number, and special character</li>
        </ul>
      </div>

      <form class="hc-form" novalidate onsubmit={submitDetails}>
        {#if serverError}
          <ServerError error={serverError} />
        {/if}

        <label class="hc-field">
          <span>Current password</span>
          <input
            autocomplete="current-password"
            bind:value={currentPassword}
            type="password"
            onblur={() => (touched = true)}
          />
          {#if touched && !currentPassword}
            <small class="hc-field__error">Current password is required.</small>
          {/if}
        </label>

        <label class="hc-field">
          <span>New password</span>
          <input
            autocomplete="new-password"
            bind:value={newPassword}
            type="password"
            onblur={() => (touched = true)}
          />
          {#if touched && !newPassword}
            <small class="hc-field__error">New password is required.</small>
          {:else if touched && newPassword.length < 8}
            <small class="hc-field__error">Password must be at least 8 characters.</small>
          {:else if touched && newPassword.length > 72}
            <small class="hc-field__error">Password must be 72 characters or fewer.</small>
          {:else if touched && !passwordPattern.test(newPassword)}
            <small class="hc-field__error"
              >Use uppercase, lowercase, number, and special characters.</small
            >
          {/if}
        </label>

        <label class="hc-field">
          <span>Confirm new password</span>
          <input
            autocomplete="new-password"
            bind:value={confirmPassword}
            type="password"
            onblur={() => (touched = true)}
          />
          {#if touched && !confirmPassword}
            <small class="hc-field__error">Confirm your new password.</small>
          {:else if touched && !passwordsMatch}
            <small class="hc-field__error">Passwords must match.</small>
          {/if}
        </label>

        <button
          class="hc-btn hc-btn--primary hc-btn--dialog-submit"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? 'Sending code…' : 'Send verification code'}
        </button>
      </form>
    {:else}
      <EmailVerificationStep
        description="We sent a 6-digit code to your email. Enter it below to confirm your password change."
        email={currentEmail}
        error={verificationError}
        {isResending}
        isSubmitting={isVerifying}
        onresend={resendCode}
        onverify={verifyCode}
        submitLabel="Confirm password change"
      />
    {/if}
  {/snippet}
</Dialog>
