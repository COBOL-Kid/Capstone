<script lang="ts">
  import type { MiscMaintenanceCostResponse } from '$lib/models/maintenance';

  import Dialog from './Dialog.svelte';

  interface Props {
    costs: MiscMaintenanceCostResponse[];
    onclose: () => void;
  }

  let { costs, onclose }: Props = $props();

  let searchQuery = $state('');

  const filteredCosts = $derived.by(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return costs;
    }
    return costs.filter(
      (cost) =>
        cost.maintTitle.toLowerCase().includes(query) ||
        (cost.maintDesc?.toLowerCase().includes(query) ?? false),
    );
  });

  function formatCostRange(low: number | null, high: number | null, avg: number | null): string {
    if (low != null && high != null) {
      return `$${low} – $${high}`;
    }
    if (avg != null) {
      return `$${avg}`;
    }
    return '—';
  }
</script>

<Dialog
  busy={false}
  closeLabel="Close maintenance costs dialog"
  labelledBy="maintenance-costs-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="maintenance-costs-modal-title">Maintenance costs</h2>
    </header>
  {/snippet}

  {#snippet children()}
    <label class="maintenance-costs-modal__search hc-field">
      <span>Search</span>
      <input
        aria-label="Search maintenance costs"
        bind:value={searchQuery}
        placeholder="Search costs..."
        type="search"
      />
    </label>

    {#if costs.length === 0}
      <p class="maintenance-costs-modal__status">
        No maintenance cost estimates are available for this vehicle.
      </p>
    {:else if filteredCosts.length === 0}
      <p class="maintenance-costs-modal__status">No items match your search.</p>
    {:else}
      <ul class="maintenance-costs-modal__list">
        {#each filteredCosts as cost (cost.miscMaintCostId)}
          <li class="maintenance-costs-modal__item">
            <h3 class="maintenance-costs-modal__item-title">{cost.maintTitle}</h3>
            {#if cost.maintDesc}
              <p class="maintenance-costs-modal__item-desc">{cost.maintDesc}</p>
            {/if}
            <dl class="maintenance-costs-modal__prices">
              <div>
                <dt>Independent shop</dt>
                <dd>
                  {formatCostRange(cost.independentLow, cost.independentHigh, cost.independentAvg)}
                </dd>
              </div>
              <div>
                <dt>Dealer</dt>
                <dd>{formatCostRange(cost.dealerLow, cost.dealerHigh, cost.dealerAvg)}</dd>
              </div>
            </dl>
          </li>
        {/each}
      </ul>
    {/if}
  {/snippet}
</Dialog>

<style>
  .maintenance-costs-modal__search {
    margin-bottom: 1rem;
  }

  .maintenance-costs-modal__status {
    margin: 0;
    color: var(--color-text-muted);
    text-align: center;
  }

  .maintenance-costs-modal__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 0.75rem;
  }

  .maintenance-costs-modal__item {
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    padding: 0.9rem 1rem;
    background: var(--color-surface-glass);
  }

  .maintenance-costs-modal__item-title {
    margin: 0 0 0.25rem;
    font-size: 1rem;
  }

  .maintenance-costs-modal__item-desc {
    margin: 0 0 0.6rem;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .maintenance-costs-modal__prices {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.6rem;
    margin: 0;
  }

  .maintenance-costs-modal__prices dt {
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--color-text-muted);
  }

  .maintenance-costs-modal__prices dd {
    margin: 0.15rem 0 0;
    font-weight: 700;
  }
</style>
