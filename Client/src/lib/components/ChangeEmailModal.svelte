<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import type { AuthErrorMessage } from '$lib/models/auth';

  import Dialog from './Dialog.svelte';
  import EmailVerificationStep from './EmailVerificationStep.svelte';
  import ServerError from './ServerError.svelte';

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

  let newEmail = $state('');
  let touched = $state(false);

  const busy = $derived(isSubmitting || isVerifying || isResending);
  const emailInvalid = $derived(
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail.trim()) || newEmail.trim().length > 254,
  );

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
    newEmail = newEmail.trim();
    touched = true;
    if (emailInvalid) {
      return;
    }
    isSubmitting = true;
    try {
      await authStore.initiateAccountChange({ changeType: 'EMAIL', newEmail });
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
  closeLabel="Close change email dialog"
  labelledBy="change-email-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <p class="hc-eyebrow">Honest Car</p>
      <h2 id="change-email-modal-title">Change Email</h2>
    </header>
  {/snippet}

  {#snippet children()}
    {#if step === 'details'}
      <form class="hc-form" novalidate onsubmit={submitDetails}>
        {#if serverError}
          <ServerError error={serverError} />
        {/if}

        <p class="hc-callout-title">Current email: {currentEmail}</p>

        <label class="hc-field">
          <span>New email address</span>
          <input
            autocomplete="email"
            bind:value={newEmail}
            maxlength="254"
            type="email"
            onblur={() => (touched = true)}
          />
          {#if touched && !newEmail.trim()}
            <small class="hc-field__error">Email is required.</small>
          {:else if touched && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail.trim())}
            <small class="hc-field__error">Enter a valid email address.</small>
          {:else if touched && newEmail.trim().length > 254}
            <small class="hc-field__error">Email must be 254 characters or fewer.</small>
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
        description="We sent a 6-digit code to your current email. Enter it below to confirm this change."
        email={currentEmail}
        error={verificationError}
        {isResending}
        isSubmitting={isVerifying}
        onresend={resendCode}
        onverify={verifyCode}
        submitLabel="Confirm email change"
      />
    {/if}
  {/snippet}
</Dialog>
