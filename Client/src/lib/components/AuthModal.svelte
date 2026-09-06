<script lang="ts">
  import { authStore } from '$lib/stores/auth.svelte';
  import type { AuthErrorMessage, AuthModalMode } from '$lib/models/auth';

  import Dialog from './Dialog.svelte';
  import ServerError from './ServerError.svelte';

  const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/;

  type Step = 'credentials' | 'verify-code';

  interface Props {
    mode: AuthModalMode;
    useRoutingLinks?: boolean;
    onclose: () => void;
    onmodechange?: (mode: AuthModalMode) => void;
  }

  let { mode, useRoutingLinks = true, onclose, onmodechange }: Props = $props();

  let step = $state<Step>('credentials');
  let isSubmitting = $state(false);
  let serverError = $state<AuthErrorMessage | null>(null);
  let verificationChallenge = $state<string | null>(null);

  let firstname = $state('');
  let lastname = $state('');
  let email = $state('');
  let password = $state('');
  let code = $state('');
  let touched = $state(false);

  const isSignup = $derived(mode === 'sign-up');
  const isVerifyStep = $derived(step === 'verify-code');
  const title = $derived(
    isVerifyStep ? 'Verify your email' : isSignup ? 'Create your account' : 'Welcome back',
  );
  const submitLabel = $derived(
    isVerifyStep ? 'Verify and continue' : isSignup ? 'Sign Up' : 'Sign In',
  );
  const alternateMode = $derived<AuthModalMode>(isSignup ? 'sign-in' : 'sign-up');
  const alternatePath = $derived(alternateMode === 'sign-up' ? '/sign-up' : '/sign-in');
  const alternateLabel = $derived(alternateMode === 'sign-up' ? 'Sign up' : 'Sign in');
  const alternatePrompt = $derived(isSignup ? 'Already have an account?' : 'Need an account?');

  const emailInvalid = $derived(
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) || email.trim().length > 254,
  );
  const passwordErrors = $derived.by(() => {
    if (!touched) {
      return [] as string[];
    }
    const errors: string[] = [];
    if (!password) {
      errors.push('Password is required.');
    }
    if (isSignup && password.length > 0 && password.length < 8) {
      errors.push('Password must be at least 8 characters.');
    }
    if (password.length > 72) {
      errors.push('Password must be 72 characters or fewer.');
    }
    if (isSignup && password.length >= 8 && !passwordPattern.test(password)) {
      errors.push('Use uppercase, lowercase, number, and special characters.');
    }
    return errors;
  });

  $effect(() => {
    // Reset state whenever the mode changes (mirrors Angular's effect).
    void mode;
    resetModalState();
  });

  function resetModalState(): void {
    serverError = null;
    step = 'credentials';
    verificationChallenge = null;
    firstname = '';
    lastname = '';
    email = '';
    password = '';
    code = '';
    touched = false;
  }

  function switchMode(): void {
    resetModalState();
    onmodechange?.(alternateMode);
  }

  async function submit(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }
    serverError = null;

    if (isVerifyStep) {
      await submitVerificationCode();
      return;
    }

    touched = true;
    const firstnameInvalid = isSignup && (!firstname.trim() || firstname.trim().length > 100);
    const lastnameInvalid = isSignup && (!lastname.trim() || lastname.trim().length > 100);
    const signupPasswordInvalid =
      isSignup && (password.length < 8 || password.length > 72 || !passwordPattern.test(password));
    const signinPasswordInvalid = !isSignup && (!password || password.length > 72);
    if (
      emailInvalid ||
      firstnameInvalid ||
      lastnameInvalid ||
      signupPasswordInvalid ||
      signinPasswordInvalid
    ) {
      return;
    }

    isSubmitting = true;
    try {
      const response = isSignup
        ? await authStore.register({
            firstname: firstname.trim(),
            lastname: lastname.trim(),
            email: email.trim(),
            password,
          })
        : await authStore.login({ email: email.trim(), password });
      if (response.verificationRequired && response.verificationChallenge) {
        verificationChallenge = response.verificationChallenge;
        step = 'verify-code';
        code = '';
        touched = false;
        return;
      }
      onclose();
    } catch (error) {
      serverError = error as AuthErrorMessage;
    } finally {
      isSubmitting = false;
    }
  }

  async function submitVerificationCode(): Promise<void> {
    const challenge = verificationChallenge;
    if (!challenge) {
      return;
    }
    touched = true;
    if (!/^\d{6}$/.test(code)) {
      return;
    }
    isSubmitting = true;
    try {
      await authStore.completeEmailVerificationSignIn(challenge, code);
      onclose();
    } catch (error) {
      serverError = error as AuthErrorMessage;
    } finally {
      isSubmitting = false;
    }
  }

  async function resendVerificationCode(): Promise<void> {
    if (isSubmitting || !isVerifyStep) {
      return;
    }
    isSubmitting = true;
    try {
      const response = await authStore.login({ email: email.trim(), password });
      if (response.verificationChallenge) {
        verificationChallenge = response.verificationChallenge;
        code = '';
        touched = false;
        serverError = null;
      }
    } catch (error) {
      serverError = error as AuthErrorMessage;
    } finally {
      isSubmitting = false;
    }
  }

  function backToCredentials(): void {
    step = 'credentials';
    verificationChallenge = null;
    serverError = null;
    code = '';
    touched = false;
  }
</script>

<Dialog
  busy={isSubmitting}
  closeLabel="Close authentication dialog"
  labelledBy="auth-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <p class="hc-eyebrow">Honest Car</p>
      <h2 id="auth-modal-title">{title}</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <form class="hc-form" novalidate onsubmit={submit}>
      {#if serverError}
        <ServerError error={serverError} />
      {/if}

      {#if isVerifyStep}
        <p class="hc-dialog__instructions">
          We sent a 6-digit verification code to your email. Enter it below to finish signing in.
          The code expires in 5 minutes.
        </p>

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
          {:else if touched && !/^\d{6}$/.test(code)}
            <small class="hc-field__error">Enter the 6-digit code from your email.</small>
          {/if}
        </label>

        <button
          class="hc-dialog-switch-button"
          disabled={isSubmitting}
          onclick={resendVerificationCode}
          type="button"
        >
          Resend code
        </button>

        <button class="hc-dialog-switch-button" onclick={backToCredentials} type="button">
          Back to sign in
        </button>
      {:else}
        {#if isSignup}
          <label class="hc-field">
            <span>First name</span>
            <input
              autocomplete="given-name"
              bind:value={firstname}
              type="text"
              onblur={() => (touched = true)}
            />
            {#if touched && !firstname.trim()}
              <small class="hc-field__error">First name is required.</small>
            {/if}
          </label>

          <label class="hc-field">
            <span>Last name</span>
            <input
              autocomplete="family-name"
              bind:value={lastname}
              type="text"
              onblur={() => (touched = true)}
            />
            {#if touched && !lastname.trim()}
              <small class="hc-field__error">Last name is required.</small>
            {/if}
          </label>
        {/if}

        <label class="hc-field">
          <span>Email</span>
          <input
            autocomplete="email"
            bind:value={email}
            type="email"
            onblur={() => (touched = true)}
          />
          {#if touched && !email.trim()}
            <small class="hc-field__error">Email is required.</small>
          {:else if touched && emailInvalid}
            <small class="hc-field__error">Enter a valid email address.</small>
          {/if}
        </label>

        <label class="hc-field">
          <span>Password</span>
          <input
            autocomplete={isSignup ? 'new-password' : 'current-password'}
            bind:value={password}
            type="password"
            onblur={() => (touched = true)}
          />
          {#each passwordErrors as error (error)}
            <small class="hc-field__error">{error}</small>
          {/each}
        </label>
      {/if}

      <button
        class="hc-btn hc-btn--primary hc-btn--dialog-submit"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? 'Please wait...' : submitLabel}
      </button>
    </form>

    {#if !isVerifyStep}
      <p class="hc-dialog-switch">
        {alternatePrompt}
        {#if useRoutingLinks}
          <a href={alternatePath}>{alternateLabel}</a>
        {:else}
          <button class="hc-dialog-switch-button" onclick={switchMode} type="button">
            {alternateLabel}
          </button>
        {/if}
      </p>
    {/if}
  {/snippet}
</Dialog>

<style>
  .hc-dialog__instructions {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    line-height: 1.5;
  }
</style>
