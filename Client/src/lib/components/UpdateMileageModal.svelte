<script lang="ts">
  import { onMount } from 'svelte';
  import { updateMileage } from '$lib/services/vin';
  import type { VehicleDetailResponse } from '$lib/models/vin';

  import Dialog from './Dialog.svelte';

  interface Props {
    vin: string;
    currentMileage: number;
    onclose: () => void;
    onupdated: (detail: VehicleDetailResponse) => void;
  }

  let { vin, currentMileage, onclose, onupdated }: Props = $props();

  let isSubmitting = $state(false);
  let serverError = $state<string | null>(null);
  let mileage = $state<number | null>(null);
  let touched = $state(false);

  const mileageInvalid = $derived(mileage === null || !Number.isFinite(mileage) || mileage < 0);

  onMount(() => {
    mileage = currentMileage;
  });

  async function submit(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    touched = true;
    if (mileageInvalid || isSubmitting) {
      return;
    }
    serverError = null;
    isSubmitting = true;
    try {
      const detail = await updateMileage(vin, { currentMileage: mileage ?? 0 });
      onupdated(detail);
      onclose();
    } catch {
      serverError = 'Unable to update mileage. Please try again.';
    } finally {
      isSubmitting = false;
    }
  }
</script>

<Dialog
  busy={isSubmitting}
  closeLabel="Close update mileage dialog"
  labelledBy="update-mileage-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="update-mileage-modal-title">Update Mileage</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <form class="hc-form" novalidate onsubmit={submit}>
      <label class="hc-field">
        <span>Current mileage</span>
        <input
          bind:value={mileage}
          min="0"
          onblur={() => (touched = true)}
          required
          type="number"
        />
        {#if touched && mileageInvalid}
          <small class="hc-field__error">Mileage cannot be negative.</small>
        {/if}
      </label>

      {#if serverError}
        <div class="hc-alert hc-alert--error" role="alert">
          <p>{serverError}</p>
        </div>
      {/if}

      <button
        class="hc-btn hc-btn--primary hc-btn--dialog-submit"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? 'Saving...' : 'Save mileage'}
      </button>
    </form>
  {/snippet}
</Dialog>
