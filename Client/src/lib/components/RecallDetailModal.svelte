<script lang="ts">
  import { localDateIso } from '$lib/utils/local-date';
  import { completeRecall, uncompleteRecall } from '$lib/services/recall';
  import type { CompletedRecallResponse, RecallResponse } from '$lib/models/recall';

  import Dialog from './Dialog.svelte';

  interface Props {
    vin: string;
    isCompleted: boolean;
    uncompletedItem?: RecallResponse | null;
    completedItem?: CompletedRecallResponse | null;
    onclose: () => void;
    onchanged: () => void;
  }

  let {
    vin,
    isCompleted,
    uncompletedItem = null,
    completedItem = null,
    onclose,
    onchanged,
  }: Props = $props();

  let isSubmitting = $state(false);
  let serverError = $state<string | null>(null);
  let showCompleteForm = $state(false);
  const maxCompletedDate = localDateIso();

  let completedDate = $state(localDateIso());
  let repairShop = $state('');
  let cost = $state<number | null>(null);
  let notes = $state('');

  const completeFormInvalid = $derived(!completedDate || completedDate > maxCompletedDate);

  function openCompleteForm(): void {
    showCompleteForm = true;
    completedDate = localDateIso();
  }

  async function markComplete(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const recall = uncompletedItem;
    if (!recall || completeFormInvalid || isSubmitting) {
      return;
    }
    serverError = null;
    isSubmitting = true;
    try {
      await completeRecall({
        vin,
        recallId: recall.recallId,
        completedDate,
        repairShop: repairShop || undefined,
        cost: cost ?? undefined,
        notes: notes || undefined,
      });
      onchanged();
      onclose();
    } catch {
      serverError = 'Unable to mark recall complete.';
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
      await uncompleteRecall(completed.completedRecallId);
      onchanged();
      onclose();
    } catch {
      serverError = 'Unable to mark recall incomplete.';
    } finally {
      isSubmitting = false;
    }
  }
</script>

<Dialog
  busy={isSubmitting}
  closeLabel="Close recall detail dialog"
  labelledBy="recall-detail-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="recall-detail-modal-title">Recall</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <div class="hc-form">
      {#if isCompleted && completedItem}
        <dl class="hc-detail-fields">
          <div>
            <dt>Campaign</dt>
            <dd>{completedItem.nhtsaCampaignNumber}</dd>
          </div>
          <div>
            <dt>Component</dt>
            <dd>{completedItem.component}</dd>
          </div>
          <div>
            <dt>Summary</dt>
            <dd>{completedItem.summary}</dd>
          </div>
          <div>
            <dt>Consequence</dt>
            <dd>{completedItem.consequence}</dd>
          </div>
          <div>
            <dt>Remedy</dt>
            <dd>{completedItem.remedy}</dd>
          </div>
          <div>
            <dt>Completed</dt>
            <dd>{completedItem.completedDate}</dd>
          </div>
          {#if completedItem.repairShop}
            <div>
              <dt>Repair shop</dt>
              <dd>{completedItem.repairShop}</dd>
            </div>
          {/if}
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
      {:else if uncompletedItem}
        <dl class="hc-detail-fields">
          <div>
            <dt>Campaign</dt>
            <dd>{uncompletedItem.nhtsaCampaignNumber}</dd>
          </div>
          <div>
            <dt>Reported</dt>
            <dd>{uncompletedItem.reportReceivedDate}</dd>
          </div>
          <div>
            <dt>Component</dt>
            <dd>{uncompletedItem.component}</dd>
          </div>
          <div>
            <dt>Summary</dt>
            <dd>{uncompletedItem.summary}</dd>
          </div>
          <div>
            <dt>Consequence</dt>
            <dd>{uncompletedItem.consequence}</dd>
          </div>
          <div>
            <dt>Remedy</dt>
            <dd>{uncompletedItem.remedy}</dd>
          </div>
        </dl>

        {#if showCompleteForm}
          <form class="hc-detail-complete-form" novalidate onsubmit={markComplete}>
            <label class="hc-field">
              <span>Completed date</span>
              <input bind:value={completedDate} max={maxCompletedDate} required type="date" />
            </label>
            <label class="hc-field">
              <span>Repair shop (optional)</span>
              <input bind:value={repairShop} type="text" />
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
