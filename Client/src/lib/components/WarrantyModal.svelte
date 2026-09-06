<script lang="ts">
  import {
    formatWarrantyCoverageLabel,
    formatWarrantyCoverageStatus,
  } from '$lib/utils/warranty-display';
  import type { VehicleWarrantyResponse } from '$lib/models/warranty';

  import Dialog from './Dialog.svelte';

  interface Props {
    warranty: VehicleWarrantyResponse;
    onclose: () => void;
  }

  let { warranty, onclose }: Props = $props();
</script>

<Dialog
  busy={false}
  closeLabel="Close warranty information dialog"
  labelledBy="warranty-modal-title"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="warranty-modal-title">Warranty information</h2>
      <p class="warranty-modal__subtitle">
        {warranty.vehicleYear}
        {warranty.vehicleMake}
        {warranty.vehicleModel}
      </p>
    </header>
  {/snippet}

  {#snippet children()}
    {#if warranty.coverages.length === 0}
      <p class="warranty-modal__status">No warranty information is available for this vehicle.</p>
    {:else}
      <dl class="warranty-modal__list">
        {#each warranty.coverages as coverage (coverage.coverageName)}
          <div class="warranty-modal__item">
            <dt>{formatWarrantyCoverageLabel(coverage.coverageName)}</dt>
            <dd>
              {coverage.coverageValue}
              {#if coverage.estimatedExpirationDate}
                <span class="warranty-modal__status">{formatWarrantyCoverageStatus(coverage)}</span>
              {/if}
            </dd>
          </div>
        {/each}
      </dl>
    {/if}
  {/snippet}
</Dialog>

<style>
  .warranty-modal__subtitle {
    margin: 0.5rem 0 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .warranty-modal__status {
    margin: 0;
    color: var(--color-text-muted);
  }

  .warranty-modal__list {
    display: grid;
    gap: 0.75rem;
    margin: 0;
  }

  .warranty-modal__item {
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    padding: 0.9rem 1rem;
    background: var(--color-surface-glass);
  }

  .warranty-modal__item dt {
    font-weight: 700;
    margin-bottom: 0.25rem;
  }

  .warranty-modal__item dd {
    margin: 0;
    color: var(--color-navy-100);
  }

  .warranty-modal__item .warranty-modal__status {
    display: block;
    margin-top: 0.25rem;
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
</style>
