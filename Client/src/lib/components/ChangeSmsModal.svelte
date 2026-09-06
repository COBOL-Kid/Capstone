<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import type { AuthErrorMessage } from '$lib/models/auth';

  import Dialog from './Dialog.svelte';
  import EmailVerificationStep from './EmailVerificationStep.svelte';
  import ServerError from './ServerError.svelte';

  const smsPattern = /^$|^(?=.*\d)[+0-9() .-]+$/;

  type Step = 'details' | 'verify';

  interface Props {
    currentEmail: string;
    currentSms?: string;
    startOnVerifyStep?: boolean;
    onclose: () => void;
    onchanged: () => void;
  }

  let {
    currentEmail,
    currentSms = '',
    startOnVerifyStep = false,
    onclose,
    onchanged,
  }: Props = $props();

  let step = $state<Step>('details');
  let isSubmitting = $state(false);
  let isVerifying = $state(false);
  let isResending = $state(false);
  let serverError = $state<AuthErrorMessage | null>(null);
  let verificationError = $state<AuthErrorMessage | null>(null);

  let userSms = $state('');
  let touched = $state(false);

  const busy = $derived(isSubmitting || isVerifying || isResending);
  const smsTooLong = $derived(userSms.trim().length > 20);
  const smsBadPattern = $derived(!smsPattern.test(userSms.trim()));
  const smsInvalid = $derived(smsTooLong || smsBadPattern);

  onMount(() => {
    userSms = currentSms;
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
    userSms = userSms.trim();
    touched = true;
    if (smsInvalid) {
      return;
    }
    isSubmitting = true;
    try {
      await authStore.initiateAccountChange({ changeType: 'SMS', userSms: userSms || null });
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

<Dialog {busy} closeLabel="Close change SMS dialog" labelledBy="change-sms-modal-title" {onclose}>
  {#snippet header()}
    <header class="hc-dialog__header">
      <p class="hc-eyebrow">Honest Car</p>
      <h2 id="change-sms-modal-title">Change SMS Number</h2>
    </header>
  {/snippet}

  {#snippet children()}
    {#if step === 'details'}
      <form class="hc-form" novalidate onsubmit={submitDetails}>
        {#if serverError}
          <ServerError error={serverError} />
        {/if}

        <p class="hc-callout-title">Current SMS: {currentSms || 'Not provided'}</p>

        <label class="hc-field">
          <span>New SMS phone number</span>
          <input
            autocomplete="tel"
            bind:value={userSms}
            maxlength="20"
            type="tel"
            onblur={() => (touched = true)}
          />
          <small>Leave blank to remove your SMS number.</small>
          {#if touched && smsTooLong}
            <small class="hc-field__error">SMS number must be 20 characters or fewer.</small>
          {:else if touched && smsBadPattern}
            <small class="hc-field__error">
              Use a phone number with digits and common phone characters.
            </small>
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
        description="We sent a 6-digit code to your email. Enter it below to confirm this SMS change."
        email={currentEmail}
        error={verificationError}
        {isResending}
        isSubmitting={isVerifying}
        onresend={resendCode}
        onverify={verifyCode}
        submitLabel="Confirm SMS change"
      />
    {/if}
  {/snippet}
</Dialog>
