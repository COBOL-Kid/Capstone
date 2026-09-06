<script lang="ts">
  import { onMount } from 'svelte';
  import { localDateIso } from '$lib/utils/local-date';
  import { completeMaintenance, uncompleteMaintenance } from '$lib/services/maintenance';
  import type {
    CompletedMaintenanceResponse,
    SelectedUpcomingMaintenance,
  } from '$lib/models/maintenance';

  import Dialog from './Dialog.svelte';

  interface Props {
    vin: string;
    currentMileage: number;
    isCompleted: boolean;
    upcomingItem?: SelectedUpcomingMaintenance | null;
    completedItem?: CompletedMaintenanceResponse | null;
    onclose: () => void;
    onchanged: () => void;
  }

  let {
    vin,
    currentMileage,
    isCompleted,
    upcomingItem = null,
    completedItem = null,
    onclose,
    onchanged,
  }: Props = $props();

  let isSubmitting = $state(false);
  let serverError = $state<string | null>(null);
  let showCompleteForm = $state(false);
  const maxCompletedDate = localDateIso();

  let completedDate = $state(localDateIso());
  let mileageCompleted = $state<number | null>(null);
  let cost = $state<number | null>(null);
  let notes = $state('');
  let touched = $state(false);

  const completeFormInvalid = $derived(
    !completedDate ||
      completedDate > maxCompletedDate ||
      mileageCompleted === null ||
      !Number.isFinite(mileageCompleted) ||
      mileageCompleted < 0,
  );

  onMount(() => {
    mileageCompleted = currentMileage;
  });

  function openCompleteForm(): void {
    showCompleteForm = true;
    completedDate = localDateIso();
    mileageCompleted = currentMileage;
  }

  async function markComplete(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const upcoming = upcomingItem;
    touched = true;
    if (!upcoming || completeFormInvalid || isSubmitting) {
      return;
    }
    serverError = null;
    isSubmitting = true;
    try {
      await completeMaintenance({
        vin,
        maintMileageId: upcoming.maintMileageId,
        completedDate,
        mileageCompleted: mileageCompleted ?? 0,
        cost: cost ?? undefined,
        notes: notes || undefined,
      });
      onchanged();
      onclose();
    } catch {
      serverError = 'Unable to mark maintenance complete.';
    } finally {
      isSubmitting = false;
    }
  }

  async function markIncomplete(): Promise<void> {
    const completed = completedItem;
    if (!completed || isSubmitting) {
      return;
    }
    serverError = null;
    isSubmitting = true;
    try {
      await uncompleteMaintenance(completed.completedMaintenanceId);
      onchanged();
      onclose();
    } catch {
      serverError = 'Unable to mark maintenance incomplete.';
    } finally {
      isSubmitting = false;
    }
  }
</script>

<Dialog
  busy={isSubmitting}
  closeLabel="Close maintenance detail dialog"
  labelledBy="maintenance-detail-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="maintenance-detail-modal-title">Maintenance</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <div class="hc-form">
      {#if isCompleted && completedItem}
        <dl class="hc-detail-fields">
          <div>
            <dt>Description</dt>
            <dd>{completedItem.maintDesc}</dd>
          </div>
          <div>
            <dt>Due at</dt>
            <dd>{completedItem.mileageDue} miles</dd>
          </div>
          <div>
            <dt>Completed</dt>
            <dd>{completedItem.completedDate} at {completedItem.mileageCompleted} miles</dd>
          </div>
          {#if completedItem.cost != null}
            <div>
              <dt>Cost</dt>
              <dd>${completedItem.cost}</dd>
            </div>
          {/if}
          {#if completedItem.notes}
            <div>
              <dt>Notes</dt>
              <dd>{completedItem.notes}</dd>
            </div>
          {/if}
        </dl>
        <button
          class="hc-btn hc-btn--primary hc-btn--dialog-submit hc-btn--danger"
          disabled={isSubmitting}
          onclick={markIncomplete}
          type="button"
        >
          {isSubmitting ? 'Updating...' : 'Mark incomplete'}
        </button>
      {:else if upcomingItem}
        <dl class="hc-detail-fields">
          <div>
            <dt>Description</dt>
            <dd>{upcomingItem.maintDesc}</dd>
          </div>
          <div>
            <dt>Due at</dt>
            <dd>{upcomingItem.mileageDue} miles</dd>
          </div>
        </dl>

        {#if showCompleteForm}
          <form class="hc-detail-complete-form" novalidate onsubmit={markComplete}>
            <label class="hc-field">
              <span>Completed date</span>
              <input
                bind:value={completedDate}
                max={maxCompletedDate}
                onblur={() => (touched = true)}
                required
                type="date"
              />
            </label>
            <label class="hc-field">
              <span>Mileage completed</span>
              <input
                bind:value={mileageCompleted}
                min="0"
                onblur={() => (touched = true)}
                required
                type="number"
              />
            </label>
            <label class="hc-field">
              <span>Cost (optional)</span>
              <input bind:value={cost} min="0" step="0.01" type="number" />
            </label>
            <label class="hc-field">
              <span>Notes (optional)</span>
              <textarea bind:value={notes} rows="3"></textarea>
            </label>
            <button
              class="hc-btn hc-btn--primary hc-btn--dialog-submit"
              disabled={isSubmitting}
              type="submit"
            >
              {isSubmitting ? 'Saving...' : 'Confirm complete'}
            </button>
          </form>
        {:else}
          <button
            class="hc-btn hc-btn--primary hc-btn--dialog-submit"
            onclick={openCompleteForm}
            type="button"
          >
            Mark complete
          </button>
        {/if}
      {/if}

      {#if serverError}
        <div class="hc-alert hc-alert--error" role="alert">
          <p>{serverError}</p>
        </div>
      {/if}
    </div>
  {/snippet}
</Dialog>
