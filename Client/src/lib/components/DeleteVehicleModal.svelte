<script lang="ts">
  import { deleteVehicle } from '$lib/services/vin';

  import Dialog from './Dialog.svelte';

  interface Props {
    vin: string;
    onclose: () => void;
    ondeleted: (vin: string) => void;
  }

  let { vin, onclose, ondeleted }: Props = $props();

  let isSubmitting = $state(false);
  let serverError = $state<string | null>(null);

  async function submit(): Promise<void> {
    if (isSubmitting) {
      return;
    }
    serverError = null;
    isSubmitting = true;
    try {
      await deleteVehicle(vin);
      ondeleted(vin);
      onclose();
    } catch {
      serverError = 'An error occurred deleting the vehicle.';
    } finally {
      isSubmitting = false;
    }
  }
</script>

<Dialog
  busy={isSubmitting}
  closeLabel="Close delete vehicle dialog"
  labelledBy="delete-vehicle-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="delete-vehicle-modal-title">Delete Vehicle</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <div class="hc-form">
      <p class="delete-vehicle-modal__confirm">
        Are you sure you want to delete the vehicle with VIN {vin}? This action cannot be undone.
      </p>

      {#if serverError}
        <div class="hc-alert hc-alert--error" role="alert">
          <p>{serverError}</p>
        </div>
      {/if}

      <button
        class="hc-btn hc-btn--primary hc-btn--dialog-submit hc-btn--danger"
        disabled={isSubmitting}
        onclick={submit}
        type="button"
      >
        {isSubmitting ? 'Deleting...' : 'Delete Vehicle'}
      </button>
      <button
        class="hc-dialog-switch-button hc-btn--block delete-vehicle-modal__cancel"
        disabled={isSubmitting}
        onclick={onclose}
        type="button"
      >
        Cancel
      </button>
    </div>
  {/snippet}
</Dialog>

<style>
  .delete-vehicle-modal__confirm {
    color: var(--color-text-muted);
    text-align: center;
    margin: 0 0 1rem;
  }

  .delete-vehicle-modal__cancel {
    margin-top: 1rem;
    width: 100%;
  }
</style>
