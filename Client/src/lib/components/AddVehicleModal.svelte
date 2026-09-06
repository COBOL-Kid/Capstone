<script lang="ts">
  import { getTrimOptions } from '$lib/services/vin';
  import type {
    AddVinRequest,
    AddVinTrimSelectionRequiredResponse,
    VinErrorMessage,
  } from '$lib/models/vin';

  import Dialog from './Dialog.svelte';
  import ServerError from './ServerError.svelte';

  const vinPattern = /^[A-HJ-NPR-Z0-9]{17}$/i;
  const vinValidationMessage = 'VIN must be 17 characters and cannot contain I, O, or Q';

  type Step = 'form' | 'limitedDataWarning' | 'trimSelection';

  interface Props {
    isSubmitting?: boolean;
    serverError?: VinErrorMessage | null;
    trimSelectionContext?: AddVinTrimSelectionRequiredResponse | null;
    onclose: () => void;
    onsubmitrequest: (request: AddVinRequest) => void;
    onservererrorclear: () => void;
  }

  let {
    isSubmitting = false,
    serverError = null,
    trimSelectionContext = null,
    onclose,
    onsubmitrequest,
    onservererrorclear,
  }: Props = $props();

  let step = $state<Step>('form');
  let trimOptions = $state<string[]>([]);
  let selectedTrim = $state('');
  let trimLoadError = $state<string | null>(null);
  let isLoadingTrims = $state(false);

  let vin = $state('');
  let currentMileage = $state<number | null>(0);
  let touched = $state(false);

  const vinInvalid = $derived(
    !vin.trim() || vin.trim().length !== 17 || !vinPattern.test(vin.trim()),
  );
  const mileageInvalid = $derived(
    currentMileage === null || !Number.isFinite(currentMileage) || currentMileage < 0,
  );
  const busy = $derived(isSubmitting || isLoadingTrims);

  $effect(() => {
    if (trimSelectionContext) {
      step = 'limitedDataWarning';
      trimOptions = [];
      selectedTrim = '';
      trimLoadError = null;
    }
  });

  function handleVinInput(): void {
    if (serverError) {
      onservererrorclear();
    }
  }

  function submit(event: SubmitEvent): void {
    event.preventDefault();
    if (isSubmitting || step !== 'form') {
      return;
    }
    touched = true;
    if (vinInvalid || mileageInvalid) {
      return;
    }
    onsubmitrequest({ vin: vin.trim().toUpperCase(), currentMileage: currentMileage ?? 0 });
  }

  function continueDespiteLimitedData(): void {
    const context = trimSelectionContext;
    if (!context) {
      return;
    }
    step = 'trimSelection';
    trimLoadError = null;
    void loadTrimOptions(context);
  }

  function onTrimFieldInteract(): void {
    const context = trimSelectionContext;
    if (!context || trimOptions.length > 0 || isLoadingTrims) {
      return;
    }
    void loadTrimOptions(context);
  }

  function confirmTrimSelection(): void {
    if (isSubmitting) {
      return;
    }
    const trim = selectedTrim.trim();
    if (!trim) {
      trimLoadError = 'Select a trim to continue.';
      return;
    }
    onsubmitrequest({
      vin: vin.trim().toUpperCase(),
      currentMileage: currentMileage ?? 0,
      selectedTrim: trim,
    });
  }

  async function loadTrimOptions(context: AddVinTrimSelectionRequiredResponse): Promise<void> {
    isLoadingTrims = true;
    trimLoadError = null;
    try {
      const trims = await getTrimOptions(context.year, context.make, context.model);
      trimOptions = trims;
      if (trims.length === 0) {
        trimLoadError = 'No trim options are available for this vehicle.';
      }
    } catch {
      trimLoadError = 'Unable to load trim options. Please try again.';
    } finally {
      isLoadingTrims = false;
    }
  }
</script>

<Dialog {busy} closeLabel="Close add vehicle dialog" labelledBy="add-vehicle-modal-title" {onclose}>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="add-vehicle-modal-title">Add a vehicle</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <form class="hc-form" novalidate onsubmit={submit}>
      {#if serverError}
        <ServerError error={serverError} />
      {/if}

      {#if step === 'limitedDataWarning'}
        <div class="hc-alert hc-alert--warning" role="status">
          <p>Full vehicle details may not be available</p>
        </div>
        <button
          class="hc-btn hc-btn--primary hc-btn--dialog-submit"
          disabled={isSubmitting || isLoadingTrims}
          onclick={continueDespiteLimitedData}
          type="button"
        >
          Continue anyways
        </button>
      {:else if step === 'trimSelection'}
        {#if isSubmitting}
          <div
            aria-busy="true"
            aria-live="polite"
            class="hc-vehicle-onboarding-status"
            role="status"
          >
            <div aria-hidden="true" class="hc-vehicle-onboarding-status__loader"></div>
            <p class="hc-vehicle-onboarding-status__message">Adding vehicle…</p>
            <p class="hc-vehicle-onboarding-status__message">Patience is a virtue…</p>
          </div>
        {/if}

        <label class="hc-field" for="add-vehicle-trim">
          <span>Trim</span>
          <select
            id="add-vehicle-trim"
            bind:value={selectedTrim}
            disabled={isSubmitting || isLoadingTrims}
            onfocus={onTrimFieldInteract}
          >
            <option disabled hidden value="">Select a trim</option>
            {#each trimOptions as trim (trim)}
              <option value={trim}>{trim}</option>
            {/each}
          </select>
          {#if isLoadingTrims}
            <small class="hc-field__error">Loading trim options…</small>
          {/if}
          {#if trimLoadError}
            <small class="hc-field__error">{trimLoadError}</small>
          {/if}
        </label>

        <button
          class="hc-btn hc-btn--primary hc-btn--dialog-submit"
          disabled={isSubmitting || isLoadingTrims}
          onclick={confirmTrimSelection}
          type="button"
        >
          Confirm
        </button>
      {:else}
        {#if isSubmitting}
          <div
            aria-busy="true"
            aria-live="polite"
            class="hc-vehicle-onboarding-status"
            role="status"
          >
            <div aria-hidden="true" class="hc-vehicle-onboarding-status__loader"></div>
            <p class="hc-vehicle-onboarding-status__message">Adding vehicle…</p>
            <p class="hc-vehicle-onboarding-status__message">This may take a while…</p>
          </div>
        {/if}

        <label class="hc-field">
          <span>VIN</span>
          <input
            bind:value={vin}
            maxlength="17"
            onblur={() => (touched = true)}
            oninput={handleVinInput}
            placeholder="17-character VIN"
            type="text"
          />
          {#if touched && !vin.trim()}
            <small class="hc-field__error">VIN is required.</small>
          {:else if touched && vinInvalid}
            <small class="hc-field__error">{vinValidationMessage}</small>
          {/if}
        </label>

        <label class="hc-field">
          <span>Current Mileage</span>
          <input
            bind:value={currentMileage}
            min="0"
            onblur={() => (touched = true)}
            type="number"
          />
          {#if touched && (currentMileage === null || !Number.isFinite(currentMileage))}
            <small class="hc-field__error">Current mileage is required.</small>
          {:else if touched && mileageInvalid}
            <small class="hc-field__error">Mileage cannot be negative.</small>
          {/if}
        </label>

        <button
          class="hc-btn hc-btn--primary hc-btn--dialog-submit"
          disabled={isSubmitting}
          type="submit"
        >
          Add Vehicle
        </button>
      {/if}
    </form>
  {/snippet}
</Dialog>
