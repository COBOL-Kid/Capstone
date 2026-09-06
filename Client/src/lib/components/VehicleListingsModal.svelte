<script lang="ts">
  import { onMount } from 'svelte';
  import { ApiError } from '$lib/api/client';
  import { isSafeHttpUrl } from '$lib/api/safe-url';
  import { getVehicleListings } from '$lib/services/vin';
  import type { VehicleListingResponse, VehicleListingsResponse } from '$lib/models/listings';

  import Dialog from './Dialog.svelte';

  interface Props {
    vin: string;
    onclose: () => void;
  }

  let { vin, onclose }: Props = $props();

  let isLoading = $state(true);
  let errorMessage = $state<string | null>(null);
  let response = $state<VehicleListingsResponse | null>(null);

  const listings = $derived(response?.listings ?? []);
  const subtitle = $derived(response ? `${response.year} ${response.make} ${response.model}` : '');
  const totalCount = $derived(response?.total ?? null);
  const pricingSummary = $derived(response?.pricingSummary ?? null);
  const hasPricingSummary = $derived(
    pricingSummary != null && pricingSummary.pricedListingCount > 0,
  );

  onMount(() => {
    void loadListings();
  });

  async function loadListings(): Promise<void> {
    isLoading = true;
    errorMessage = null;
    try {
      response = await getVehicleListings(vin);
    } catch (error) {
      response = null;
      errorMessage = toErrorMessage(error);
    } finally {
      isLoading = false;
    }
  }

  function toErrorMessage(error: unknown): string {
    if (error instanceof ApiError) {
      if (error.status === 429 && typeof error.body === 'string' && error.body.trim()) {
        return error.body.trim();
      }
    }
    return 'Unable to load vehicle listings right now. Please try again later.';
  }

  function formatPrice(price: number | null): string {
    if (price == null) {
      return '—';
    }
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 0,
    }).format(price);
  }

  function formatMiles(miles: number | null): string {
    if (miles == null) {
      return '—';
    }
    return new Intl.NumberFormat('en-US').format(miles);
  }

  function formatLocation(listing: VehicleListingResponse): string {
    const parts = [listing.city, listing.state].filter((part) => part != null && part !== '');
    return parts.join(', ');
  }

  function formatHistorySummary(listing: VehicleListingResponse): string | null {
    const history = listing.history;
    if (!history) {
      return null;
    }
    const parts: string[] = [];
    if (history.accidents === true) {
      const count = history.accidentCount;
      parts.push(
        count != null && count > 0
          ? `${count} accident${count === 1 ? '' : 's'}`
          : 'Accidents reported',
      );
    } else if (history.accidents === false) {
      parts.push('No accidents reported');
    }
    if (history.oneOwner === true) {
      parts.push('One owner');
    } else if (history.ownerCount != null && history.ownerCount > 0) {
      parts.push(`${history.ownerCount} owner${history.ownerCount === 1 ? '' : 's'}`);
    }
    if (history.usageType) {
      parts.push(history.usageType);
    }
    return parts.length > 0 ? parts.join(' · ') : null;
  }

  function openExternalUrl(url: string): void {
    if (!isSafeHttpUrl(url)) {
      return;
    }
    const tab = window.open(url, '_blank');
    if (tab) {
      tab.opener = null;
    }
  }
</script>

<Dialog
  busy={isLoading}
  closeLabel="Close current market value dialog"
  labelledBy="vehicle-listings-modal-title"
  maxWidth="36rem"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="vehicle-listings-modal-title">Current market value</h2>
      {#if subtitle}
        <p class="vehicle-listings-modal__subtitle">{subtitle}</p>
      {/if}
    </header>
  {/snippet}

  {#snippet children()}
    {#if isLoading}
      <p aria-busy="true" class="vehicle-listings-modal__status" role="status">Loading listings…</p>
      <ul aria-hidden="true" class="vehicle-listings-modal__skeleton-list">
        {#each [1, 2, 3] as placeholder (placeholder)}
          <li class="vehicle-listings-modal__skeleton-item hc-skeleton"></li>
        {/each}
      </ul>
    {:else if errorMessage}
      <p class="vehicle-listings-modal__status vehicle-listings-modal__status--error" role="alert">
        {errorMessage}
      </p>
    {:else if listings.length === 0}
      <p class="vehicle-listings-modal__status">No comparable vehicles are listed right now.</p>
    {:else}
      <div class="vehicle-listings-modal__body">
        {#if totalCount != null}
          <p class="vehicle-listings-modal__summary">
            {totalCount.toLocaleString('en-US')} comparable listing{totalCount === 1 ? '' : 's'} found
          </p>
        {/if}

        {#if hasPricingSummary && pricingSummary}
          <dl
            aria-label="Comparable listing ask prices"
            class="vehicle-listings-modal__pricing"
            role="group"
          >
            <div>
              <dt>Low</dt>
              <dd>{formatPrice(pricingSummary.minPrice)}</dd>
            </div>
            <div>
              <dt>Average</dt>
              <dd>{formatPrice(pricingSummary.averagePrice)}</dd>
            </div>
            <div>
              <dt>High</dt>
              <dd>{formatPrice(pricingSummary.maxPrice)}</dd>
            </div>
          </dl>
          <p class="vehicle-listings-modal__pricing-note">
            Based on {pricingSummary.pricedListingCount.toLocaleString('en-US')} listing{pricingSummary.pricedListingCount ===
            1
              ? ''
              : 's'}
            with prices
          </p>
        {/if}

        <ul class="vehicle-listings-modal__list">
          {#each listings as listing (listing.vin)}
            <li class="vehicle-listings-modal__item">
              <div class="vehicle-listings-modal__item-body">
                <div class="vehicle-listings-modal__item-header">
                  <h3 class="vehicle-listings-modal__item-title">
                    {listing.year}
                    {listing.make}
                    {listing.model}
                  </h3>
                  {#if listing.cpo}
                    <span class="vehicle-listings-modal__badge">CPO</span>
                  {/if}
                </div>

                {#if listing.style}
                  <p class="vehicle-listings-modal__item-style">{listing.style}</p>
                {/if}

                <p class="vehicle-listings-modal__item-price">
                  {formatPrice(listing.price)}
                  {#if listing.miles != null}
                    <span class="vehicle-listings-modal__item-miles"
                      >· {formatMiles(listing.miles)} mi</span
                    >
                  {/if}
                </p>

                {#if listing.dealer || formatLocation(listing)}
                  <p class="vehicle-listings-modal__item-dealer">
                    {#if listing.dealer}
                      <span>{listing.dealer}</span>
                    {/if}
                    {#if formatLocation(listing)}
                      <span>{formatLocation(listing)}</span>
                    {/if}
                  </p>
                {/if}

                {#if formatHistorySummary(listing)}
                  <p class="vehicle-listings-modal__item-history">
                    {formatHistorySummary(listing)}
                  </p>
                {/if}

                {#if isSafeHttpUrl(listing.vdp)}
                  <div class="vehicle-listings-modal__item-links">
                    <button
                      class="vehicle-listings-modal__link-btn"
                      onclick={() => listing.vdp && openExternalUrl(listing.vdp)}
                      type="button"
                    >
                      View listing
                    </button>
                  </div>
                {/if}
              </div>
            </li>
          {/each}
        </ul>
      </div>
    {/if}
  {/snippet}
</Dialog>

<style>
  .vehicle-listings-modal__subtitle {
    margin: 0.5rem 0 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .vehicle-listings-modal__status {
    margin: 0;
    color: var(--color-text-muted);
    text-align: center;
  }

  .vehicle-listings-modal__status--error {
    color: var(--color-alert-error-text);
  }

  .vehicle-listings-modal__skeleton-list {
    list-style: none;
    margin: 1rem 0 0;
    padding: 0;
    display: grid;
    gap: 0.75rem;
  }

  .vehicle-listings-modal__skeleton-item {
    height: 6rem;
  }

  .vehicle-listings-modal__summary {
    margin: 0 0 0.75rem;
    font-weight: 700;
  }

  .vehicle-listings-modal__pricing {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 0.6rem;
    margin: 0 0 0.35rem;
    padding: 0.9rem 1rem;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    background: var(--color-surface-glass);
  }

  .vehicle-listings-modal__pricing dt {
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--color-text-muted);
  }

  .vehicle-listings-modal__pricing dd {
    margin: 0.15rem 0 0;
    font-weight: 700;
    font-size: 1.1rem;
  }

  .vehicle-listings-modal__pricing-note {
    margin: 0 0 1rem;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .vehicle-listings-modal__list {
    flex: 1;
    min-height: 0;
    list-style: none;
    margin: 0.75rem 0 0;
    padding: 0;
    max-height: min(55vh, 28rem);
    overflow-y: auto;
    scrollbar-gutter: stable;
    display: grid;
    gap: 0.75rem;
  }

  .vehicle-listings-modal__item {
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    padding: 0.9rem 1rem;
    background: var(--color-surface-glass);
  }

  .vehicle-listings-modal__item-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .vehicle-listings-modal__item-title {
    margin: 0;
    font-size: 1rem;
  }

  .vehicle-listings-modal__badge {
    padding: 0.1rem 0.5rem;
    border-radius: 999px;
    background: var(--color-accent-tint);
    border: 1px solid var(--color-border-strong);
    color: var(--color-blue-200);
    font-size: var(--text-xs);
    font-weight: 700;
  }

  .vehicle-listings-modal__item-style,
  .vehicle-listings-modal__item-dealer,
  .vehicle-listings-modal__item-history {
    margin: 0.25rem 0 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .vehicle-listings-modal__item-price {
    margin: 0.4rem 0 0;
    font-weight: 800;
    font-size: 1.15rem;
  }

  .vehicle-listings-modal__item-miles {
    font-weight: 500;
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .vehicle-listings-modal__item-links {
    margin-top: 0.6rem;
  }

  .vehicle-listings-modal__link-btn {
    border: 1px solid var(--color-blue-600);
    border-radius: 999px;
    background: transparent;
    color: var(--color-text-on-dark);
    padding: 0.45rem 1rem;
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
  }

  .vehicle-listings-modal__link-btn:hover {
    border-color: var(--color-blue-300);
    background: rgba(245, 249, 255, 0.08);
  }
</style>
