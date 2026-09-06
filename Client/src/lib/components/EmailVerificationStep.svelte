<script lang="ts">
  import type { AuthErrorMessage } from '$lib/models/auth';

  import ServerError from './ServerError.svelte';

  interface Props {
    email?: string;
    description?: string;
    isSubmitting?: boolean;
    isResending?: boolean;
    error?: AuthErrorMessage | null;
    submitLabel?: string;
    resendLabel?: string;
    showCancel?: boolean;
    onverify: (code: string) => void;
    onresend: () => void;
    oncancel?: () => void;
  }

  let {
    email = '',
    description = 'We sent a 6-digit code to your email. Enter it below to complete this change.',
    isSubmitting = false,
    isResending = false,
    error = null,
    submitLabel = 'Verify',
    resendLabel = 'Resend code',
    showCancel = false,
    onverify,
    onresend,
    oncancel,
  }: Props = $props();

  let code = $state('');
  let touched = $state(false);

  const codeInvalid = $derived(!/^\d{6}$/.test(code));

  function submit(event: SubmitEvent): void {
    event.preventDefault();
    if (isSubmitting || isResending) {
      return;
    }
    touched = true;
    if (codeInvalid) {
      return;
    }
    onverify(code);
  }

  function handleResend(): void {
    if (isResending || isSubmitting) {
      return;
    }
    onresend();
  }
</script>

{#if email}
  <p class="email-verification-step__email">Code sent to {email}</p>
{/if}
<p class="email-verification-step__description">{description}</p>

<form class="email-verification-step hc-form" novalidate onsubmit={submit}>
  {#if error}
    <ServerError {error} />
  {/if}

  <label class="hc-field">
    <span>Verification code</span>
    <input
      autocomplete="one-time-code"
      bind:value={code}
      inputmode="numeric"
      maxlength="6"
      pattern="[0-9]*"
      type="text"
      onblur={() => (touched = true)}
    />
    {#if touched && !code}
      <small class="hc-field__error">Verification code is required.</small>
    {:else if touched && codeInvalid}
      <small class="hc-field__error">Enter the 6-digit code from your email.</small>
    {/if}
  </label>

  <div class="email-verification-step__actions">
    <button class="hc-btn hc-btn--primary" disabled={isSubmitting || isResending} type="submit">
      {isSubmitting ? 'Verifying…' : submitLabel}
    </button>
    <button
      class="hc-btn hc-btn--secondary"
      disabled={isResending || isSubmitting}
      onclick={handleResend}
      type="button"
    >
      {isResending ? 'Sending…' : resendLabel}
    </button>
    {#if showCancel}
      <button class="hc-dialog-switch-button" onclick={() => oncancel?.()} type="button">
        Cancel
      </button>
    {/if}
  </div>
</form>

<style>
  .email-verification-step__email {
    margin: 0 0 0.5rem;
    color: var(--color-blue-200);
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .email-verification-step__description {
    margin: 0 0 0.5rem;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    line-height: 1.5;
  }

  .email-verification-step__actions {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
</style>
