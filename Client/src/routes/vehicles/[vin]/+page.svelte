<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { ApiError } from '$lib/api/client';
  import { isSafeHttpUrl } from '$lib/api/safe-url';
  import { toastStore } from '$lib/stores/toast.svelte';
  import { vehiclesStore } from '$lib/stores/vehicles.svelte';
  import { loadVehiclePage } from '$lib/services/vehicle-page';
  import {
    formatWarrantyCoverageLabelForSpecs,
    formatWarrantyCoverageStatus,
  } from '$lib/utils/warranty-display';
  import type {
    CompletedMaintenanceResponse,
    LaborCostResponse,
    PartCostResponse,
    SelectedUpcomingMaintenance,
  } from '$lib/models/maintenance';
  import type { CompletedRecallResponse, RecallResponse } from '$lib/models/recall';
  import type { VehicleDetailResponse } from '$lib/models/vin';
  import type { VehiclePageData } from '$lib/models/vehicle-page';

  import MaintenanceCostsModal from '$lib/components/MaintenanceCostsModal.svelte';
  import MaintenanceDetailModal from '$lib/components/MaintenanceDetailModal.svelte';
  import RecallDetailModal from '$lib/components/RecallDetailModal.svelte';
  import UpdateMileageModal from '$lib/components/UpdateMileageModal.svelte';
  import VehicleListingsModal from '$lib/components/VehicleListingsModal.svelte';
  import VehiclePhotoModal from '$lib/components/VehiclePhotoModal.svelte';
  import WarrantyModal from '$lib/components/WarrantyModal.svelte';

  type ActiveSection = 'maintenance' | 'recalls';

  let pageData = $state<VehiclePageData | null>(null);
  let activeSection = $state<ActiveSection>('maintenance');
  let isMileageModalOpen = $state(false);
  let isPhotoModalOpen = $state(false);
  let isMaintenanceCostsModalOpen = $state(false);
  let isWarrantyModalOpen = $state(false);
  let isListingsModalOpen = $state(false);
  let selectedUpcomingMaintenance = $state<SelectedUpcomingMaintenance | null>(null);
  let selectedCompletedMaintenance = $state<CompletedMaintenanceResponse | null>(null);
  let selectedUncompletedRecall = $state<RecallResponse | null>(null);
  let selectedCompletedRecall = $state<CompletedRecallResponse | null>(null);
  let showInspectItems = $state(true);
  let isLoading = $state(true);
  let loadError = $state<string | null>(null);

  const vin = $derived($page.params.vin ?? '');
  const vehicle = $derived(pageData?.detail ?? null);
  const upcomingIntervals = $derived(pageData?.upcomingIntervals ?? []);
  const completedMaintenance = $derived(pageData?.completedMaintenance ?? []);
  const uncompletedRecalls = $derived(pageData?.uncompletedRecalls ?? []);
  const completedRecalls = $derived(pageData?.completedRecalls ?? []);
  const upcomingItemCount = $derived(
    upcomingIntervals.reduce((total, interval) => total + interval.items.length, 0),
  );
  const miscMaintenanceCosts = $derived(pageData?.miscMaintenanceCosts ?? []);
  const hasMaintenanceCostEstimates = $derived(miscMaintenanceCosts.length > 0);
  const vehicleWarranty = $derived(pageData?.vehicleWarranty ?? null);
  const hasWarrantyInformation = $derived(vehicleWarranty !== null);
  const hasOwnersManual = $derived(isSafeHttpUrl(vehicle?.ownersManual));
  const informationUnavailableMessage = 'information not yet available for this vehicle';
  const warrantyCoveragesForDisplay = $derived(
    (vehicleWarranty?.coverages ?? []).filter(
      (coverage) => coverage.estimatedExpirationDate != null,
    ),
  );

  onMount(() => {
    void fetchPageData();
  });

  $effect(() => {
    const currentVin = vin;
    if (currentVin) {
      closeAllModals();
      void fetchPageData();
    }
  });

  async function fetchPageData(): Promise<void> {
    const currentVin = vin;
    if (!currentVin) {
      return;
    }
    isLoading = true;
    loadError = null;
    try {
      pageData = await loadVehiclePage(currentVin);
    } catch (error) {
      pageData = null;
      loadError =
        error instanceof ApiError && error.status === 404
          ? 'Vehicle not found.'
          : 'Failed to load vehicle.';
    } finally {
      isLoading = false;
    }
  }

  async function refreshPageData(): Promise<void> {
    const currentVin = vin;
    if (!currentVin) {
      return;
    }
    const preservedSelectedImageUrl =
      pageData?.detail.vin === currentVin ? pageData?.detail.selectedImageUrl : undefined;
    try {
      const data = await loadVehiclePage(currentVin);
      if (
        preservedSelectedImageUrl &&
        data.detail.vin === currentVin &&
        data.detail.selectedImageUrl !== preservedSelectedImageUrl
      ) {
        pageData = {
          ...data,
          detail: { ...data.detail, selectedImageUrl: preservedSelectedImageUrl },
        };
        return;
      }
      pageData = data;
    } catch {
      // Keep stale data on refresh failure; toasts already confirm the mutation.
    }
  }

  function formatCurrency(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
    }).format(amount);
  }

  function formatNumber(value: number): string {
    return new Intl.NumberFormat('en-US').format(value);
  }

  function formatLaborCost(labor: LaborCostResponse | null): string {
    if (!labor) {
      return '';
    }
    return `${formatCurrency(labor.totalCost, labor.currency)} (${labor.timeRequiredHours}h @ ${formatCurrency(labor.hourlyRate, labor.currency)}/hr)`;
  }

  function formatPartsCost(parts: PartCostResponse[]): string {
    const total = parts.reduce((sum, part) => sum + part.totalCost, 0);
    const currency = parts[0]?.currency ?? 'USD';
    return `Parts ${formatCurrency(total, currency)}`;
  }

  function setActiveSection(section: ActiveSection): void {
    activeSection = section;
  }

  function openOwnersManual(): void {
    const url = vehicle?.ownersManual;
    if (!isSafeHttpUrl(url)) {
      return;
    }
    const tab = window.open(url, '_blank');
    if (tab) {
      tab.opener = null;
    }
  }

  function onMileageUpdated(_detail: VehicleDetailResponse): void {
    isMileageModalOpen = false;
    toastStore.success('Mileage updated.');
    void refreshPageData();
  }

  function onPhotoUpdated(selectedImageUrl: string): void {
    const data = pageData;
    if (!data) {
      return;
    }
    const photoVin = data.detail.vin;
    pageData = { ...data, detail: { ...data.detail, selectedImageUrl } };
    vehiclesStore.updateSelectedPhoto(photoVin, selectedImageUrl);
    isPhotoModalOpen = false;
    toastStore.success('Vehicle photo updated.');
    void refreshPageData();
  }

  function openUpcomingMaintenanceFromItem(
    mileageDue: number,
    item: { maintMileageId: number; maintDesc: string },
    event: Event,
  ): void {
    event.preventDefault();
    event.stopPropagation();
    selectedUpcomingMaintenance = {
      maintMileageId: item.maintMileageId,
      mileageDue,
      maintDesc: item.maintDesc,
    };
    selectedCompletedMaintenance = null;
  }

  function onMaintenanceChanged(): void {
    toastStore.success('Maintenance record updated.');
    void refreshPageData();
  }

  function onRecallChanged(): void {
    toastStore.success('Recall record updated.');
    void refreshPageData();
  }

  function closeMaintenanceModal(): void {
    selectedUpcomingMaintenance = null;
    selectedCompletedMaintenance = null;
  }

  function closeRecallModal(): void {
    selectedUncompletedRecall = null;
    selectedCompletedRecall = null;
  }

  function closeAllModals(): void {
    closeMaintenanceModal();
    closeRecallModal();
    isMaintenanceCostsModalOpen = false;
    isWarrantyModalOpen = false;
    isPhotoModalOpen = false;
    isMileageModalOpen = false;
    isListingsModalOpen = false;
  }
</script>

<svelte:head>
  <title>Vehicle · Honest Car</title>
</svelte:head>

<section aria-labelledby="vehicle-detail-title" class="vehicle-detail hc-page-shell">
  <div class="vehicle-detail__content hc-page-shell__inner">
    <a class="vehicle-detail__back" href="/home">← Back to vehicles</a>

    {#if loadError}
      <div class="vehicle-detail__status vehicle-detail__status--error hc-state-message">
        {loadError}
      </div>
    {:else if isLoading}
      <span class="hc-sr-only" role="status">Loading vehicle…</span>
      <div aria-hidden="true" class="vehicle-detail__skeleton">
        <div class="hc-skeleton vehicle-detail__skeleton-title"></div>
        <div class="vehicle-detail__header">
          <div class="hc-skeleton vehicle-detail__skeleton-image"></div>
          <div class="vehicle-detail__details-column">
            <div class="hc-skeleton vehicle-detail__skeleton-specs"></div>
            <div class="hc-skeleton vehicle-detail__skeleton-btn"></div>
            <div class="hc-skeleton vehicle-detail__skeleton-btn"></div>
            <div class="hc-skeleton vehicle-detail__skeleton-toggle"></div>
          </div>
        </div>
        <div class="hc-skeleton vehicle-detail__skeleton-section"></div>
      </div>
    {:else if vehicle}
      <h1 class="vehicle-detail__title" id="vehicle-detail-title">
        {vehicle.vehicleYear}
        {vehicle.vehicleMake}
        {vehicle.vehicleModel}
      </h1>

      <div class="vehicle-detail__header">
        <div class="vehicle-detail__image-wrap hc-surface">
          {#if vehicle.selectedImageUrl}
            <img
              alt="Photo of {vehicle.vehicleYear} {vehicle.vehicleMake} {vehicle.vehicleModel}"
              class="vehicle-detail__image"
              src={vehicle.selectedImageUrl}
            />
          {:else}
            <div class="vehicle-detail__image-placeholder">No Image</div>
          {/if}
          {#if vehicle.availableImageUrls.length > 0}
            <button
              aria-label="Change vehicle photo"
              class="vehicle-detail__photo-btn"
              onclick={() => (isPhotoModalOpen = true)}
              type="button"
            >
              <svg
                aria-hidden="true"
                class="vehicle-detail__photo-icon"
                fill="none"
                height="20"
                viewBox="0 0 24 24"
                width="20"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M4 7a2 2 0 0 1 2-2h2l1-2h6l1 2h2a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7Z"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.75"
                />
                <circle cx="12" cy="13" r="3.5" stroke="currentColor" stroke-width="1.75" />
              </svg>
            </button>
          {/if}
        </div>
        <div class="vehicle-detail__details-column">
          <div class="vehicle-detail__specs hc-surface">
            <p class="vehicle-detail__mileage">
              <span class="vehicle-detail__mileage-value"
                >{formatNumber(vehicle.currentMileage)} mi</span
              >
              <button
                class="vehicle-detail__mileage-btn"
                onclick={() => (isMileageModalOpen = true)}
                type="button"
              >
                Update
              </button>
            </p>
            <dl class="vehicle-detail__dl">
              <div class="vehicle-detail__dl-row vehicle-detail__dl-row--full">
                <dt>VIN</dt>
                <dd>{vehicle.vin}</dd>
              </div>
              <div>
                <dt>Trim</dt>
                <dd>{vehicle.vehicleTrim}</dd>
              </div>
              {#if vehicle.vehicleStyle}
                <div>
                  <dt>Style</dt>
                  <dd>{vehicle.vehicleStyle}</dd>
                </div>
              {/if}
              {#if vehicle.body}
                <div>
                  <dt>Body</dt>
                  <dd>{vehicle.body}</dd>
                </div>
              {/if}
              {#if vehicle.engineDescription}
                <div>
                  <dt>Engine</dt>
                  <dd>{vehicle.engineDescription}</dd>
                </div>
              {/if}
              {#if vehicle.transmissionStyle}
                <div>
                  <dt>Trans.</dt>
                  <dd>{vehicle.transmissionStyle}</dd>
                </div>
              {/if}
              {#if vehicle.driveType}
                <div>
                  <dt>Drive</dt>
                  <dd>{vehicle.driveType}</dd>
                </div>
              {/if}
              {#each warrantyCoveragesForDisplay as coverage (coverage.coverageName)}
                <div class="vehicle-detail__dl-row vehicle-detail__dl-row--full">
                  <dt>{formatWarrantyCoverageLabelForSpecs(coverage.coverageName)}</dt>
                  <dd>{formatWarrantyCoverageStatus(coverage)}</dd>
                </div>
              {/each}
            </dl>
            <div class="vehicle-detail__specs-actions">
              <span
                class="vehicle-detail__manual-btn-wrap"
                title={!hasOwnersManual ? informationUnavailableMessage : undefined}
              >
                <button
                  class="vehicle-detail__manual-btn"
                  class:vehicle-detail__manual-btn--unavailable={!hasOwnersManual}
                  disabled={!hasOwnersManual}
                  onclick={openOwnersManual}
                  type="button"
                >
                  Owner's manual
                </button>
              </span>
              <span
                class="vehicle-detail__manual-btn-wrap"
                title={!hasWarrantyInformation ? informationUnavailableMessage : undefined}
              >
                <button
                  class="vehicle-detail__manual-btn"
                  class:vehicle-detail__manual-btn--unavailable={!hasWarrantyInformation}
                  disabled={!hasWarrantyInformation}
                  onclick={() => hasWarrantyInformation && (isWarrantyModalOpen = true)}
                  type="button"
                >
                  Warranty information
                </button>
              </span>
            </div>
          </div>

          <span class="vehicle-detail__costs-btn-wrap">
            <button
              class="vehicle-detail__costs-btn"
              onclick={() => (isListingsModalOpen = true)}
              type="button"
            >
              Current Market Value
            </button>
          </span>

          <span
            class="vehicle-detail__costs-btn-wrap"
            title={!hasMaintenanceCostEstimates ? informationUnavailableMessage : undefined}
          >
            <button
              class="vehicle-detail__costs-btn"
              class:vehicle-detail__costs-btn--unavailable={!hasMaintenanceCostEstimates}
              disabled={!hasMaintenanceCostEstimates}
              onclick={() => hasMaintenanceCostEstimates && (isMaintenanceCostsModalOpen = true)}
              type="button"
            >
              Maintenance costs
            </button>
          </span>

          <div class="vehicle-detail__toggle" data-active={activeSection} role="tablist">
            <button
              class="vehicle-detail__toggle-btn"
              class:vehicle-detail__toggle-btn--active={activeSection === 'maintenance'}
              onclick={() => setActiveSection('maintenance')}
              type="button"
            >
              Maintenance
            </button>
            <button
              class="vehicle-detail__toggle-btn"
              class:vehicle-detail__toggle-btn--active={activeSection === 'recalls'}
              onclick={() => setActiveSection('recalls')}
              type="button"
            >
              Recalls
            </button>
          </div>
        </div>
      </div>

      {#if activeSection === 'maintenance'}
        <div class="vehicle-detail__panel">
          <details class="vehicle-detail__section hc-surface" open>
            <summary class="vehicle-detail__section-summary">
              <span class="vehicle-detail__section-title">Upcoming maintenance</span>
              {#if upcomingItemCount > 0}
                <span class="vehicle-detail__section-count">{upcomingItemCount}</span>
              {/if}
              <span aria-hidden="true" class="vehicle-detail__chevron"></span>
            </summary>
            <div class="vehicle-detail__section-body">
              {#if upcomingIntervals.length === 0}
                <div class="hc-empty-state hc-empty-state--compact">
                  <span class="hc-empty-state__icon" aria-hidden="true">
                    <svg fill="none" height="22" viewBox="0 0 24 24" width="22">
                      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                      <path
                        d="m8.5 12 2.3 2.3 4.7-4.6"
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="1.8"
                      />
                    </svg>
                  </span>
                  <p class="hc-empty-state__title">All caught up</p>
                  <p class="hc-empty-state__text">No upcoming maintenance is due right now.</p>
                </div>
              {:else}
                <label class="vehicle-detail__inspect-filter">
                  <input bind:checked={showInspectItems} type="checkbox" />
                  <span class="vehicle-detail__inspect-track" aria-hidden="true"></span>
                  Show inspections
                </label>
                <div class="vehicle-detail__mileage-groups">
                  {#each upcomingIntervals as interval (interval.mileageDue)}
                    <section class="vehicle-detail__mileage-group">
                      <h3 class="vehicle-detail__mileage-heading">
                        {formatNumber(interval.mileageDue)} mi service
                      </h3>
                      <ul class="vehicle-detail__list">
                        {#each interval.items as item (item.maintMileageId)}
                          {#if !item.isInspect || showInspectItems}
                            <li class="vehicle-detail__maint-item">
                              <div class="vehicle-detail__item-head">
                                <span class="vehicle-detail__maint-summary-main">
                                  <span class="vehicle-detail__maint-desc">{item.maintDesc}</span>
                                  <span class="vehicle-detail__maint-meta">
                                    {#if item.isInspect}
                                      <span class="vehicle-detail__maint-badge">Inspection</span>
                                    {/if}
                                    {#if item.parts.length > 0}
                                      <span class="vehicle-detail__maint-labor"
                                        >{formatPartsCost(item.parts)}</span
                                      >
                                    {/if}
                                    {#if item.labor}
                                      <span class="vehicle-detail__maint-labor"
                                        >{formatLaborCost(item.labor)}</span
                                      >
                                    {:else if item.parts.length === 0}
                                      <span class="vehicle-detail__maint-labor"
                                        >No cost breakdown available</span
                                      >
                                    {/if}
                                  </span>
                                </span>
                                <button
                                  class="vehicle-detail__maint-action"
                                  onclick={(event) =>
                                    openUpcomingMaintenanceFromItem(
                                      interval.mileageDue,
                                      item,
                                      event,
                                    )}
                                  type="button"
                                >
                                  Mark complete
                                </button>
                              </div>
                            </li>
                          {/if}
                        {/each}
                      </ul>
                      {#if interval.summary}
                        <dl class="vehicle-detail__mileage-summary">
                          <div>
                            <dt>Parts</dt>
                            <dd>
                              {formatCurrency(
                                interval.summary.totalPartsCost,
                                interval.summary.currency,
                              )}
                            </dd>
                          </div>
                          <div>
                            <dt>Labor</dt>
                            <dd>
                              {formatCurrency(
                                interval.summary.totalLaborCost,
                                interval.summary.currency,
                              )}
                            </dd>
                          </div>
                          <div class="vehicle-detail__mileage-summary-total">
                            <dt>Total</dt>
                            <dd>
                              {formatCurrency(
                                interval.summary.totalCost,
                                interval.summary.currency,
                              )}
                            </dd>
                          </div>
                        </dl>
                      {/if}
                    </section>
                  {/each}
                </div>
              {/if}
            </div>
          </details>

          <details class="vehicle-detail__section hc-surface">
            <summary class="vehicle-detail__section-summary">
              <span class="vehicle-detail__section-title">Completed maintenance</span>
              {#if completedMaintenance.length > 0}
                <span class="vehicle-detail__section-count">{completedMaintenance.length}</span>
              {/if}
              <span aria-hidden="true" class="vehicle-detail__chevron"></span>
            </summary>
            <div class="vehicle-detail__section-body">
              {#if completedMaintenance.length === 0}
                <div class="hc-empty-state hc-empty-state--compact">
                  <span class="hc-empty-state__icon" aria-hidden="true">
                    <svg fill="none" height="22" viewBox="0 0 24 24" width="22">
                      <path
                        d="M12 7v5l3 2"
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="1.7"
                      />
                      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                    </svg>
                  </span>
                  <p class="hc-empty-state__title">Nothing logged yet</p>
                  <p class="hc-empty-state__text">
                    Completed maintenance you mark will appear here.
                  </p>
                </div>
              {:else}
                <ul class="vehicle-detail__rows">
                  {#each completedMaintenance as item (item.completedMaintenanceId)}
                    <li>
                      <button
                        class="vehicle-detail__row-btn"
                        onclick={() => {
                          selectedCompletedMaintenance = item;
                          selectedUpcomingMaintenance = null;
                        }}
                        type="button"
                      >
                        <span class="vehicle-detail__row-main">
                          <span class="vehicle-detail__row-title">{item.maintDesc}</span>
                          <span class="vehicle-detail__row-meta">
                            {item.completedDate} · {item.mileageCompleted} mi
                          </span>
                        </span>
                        <span aria-hidden="true" class="vehicle-detail__row-arrow"></span>
                      </button>
                    </li>
                  {/each}
                </ul>
              {/if}
            </div>
          </details>
        </div>
      {:else}
        <div class="vehicle-detail__panel">
          <details class="vehicle-detail__section hc-surface" open>
            <summary class="vehicle-detail__section-summary">
              <span class="vehicle-detail__section-title">Open recalls</span>
              {#if uncompletedRecalls.length > 0}
                <span class="vehicle-detail__section-count vehicle-detail__section-count--alert">
                  {uncompletedRecalls.length}
                </span>
              {/if}
              <span aria-hidden="true" class="vehicle-detail__chevron"></span>
            </summary>
            <div class="vehicle-detail__section-body">
              {#if uncompletedRecalls.length === 0}
                <div class="hc-empty-state hc-empty-state--compact">
                  <span class="hc-empty-state__icon" aria-hidden="true">
                    <svg fill="none" height="22" viewBox="0 0 24 24" width="22">
                      <path
                        d="M12 3.5 5 6.3v4.8c0 4.2 2.9 7.3 7 8.4 4.1-1.1 7-4.2 7-8.4V6.3L12 3.5Z"
                        stroke="currentColor"
                        stroke-linejoin="round"
                        stroke-width="1.5"
                      />
                      <path
                        d="m9.2 12 1.9 1.9 3.7-3.8"
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="1.7"
                      />
                    </svg>
                  </span>
                  <p class="hc-empty-state__title">In the clear</p>
                  <p class="hc-empty-state__text">No open recalls for this vehicle.</p>
                </div>
              {:else}
                <ul class="vehicle-detail__rows">
                  {#each uncompletedRecalls as item (item.recallId)}
                    <li>
                      <button
                        class="vehicle-detail__row-btn"
                        onclick={() => {
                          selectedUncompletedRecall = item;
                          selectedCompletedRecall = null;
                        }}
                        type="button"
                      >
                        <span class="vehicle-detail__row-main">
                          <span class="vehicle-detail__row-title">{item.component}</span>
                          <span class="vehicle-detail__row-meta">{item.nhtsaCampaignNumber}</span>
                        </span>
                        <span aria-hidden="true" class="vehicle-detail__row-arrow"></span>
                      </button>
                    </li>
                  {/each}
                </ul>
              {/if}
            </div>
          </details>

          <details class="vehicle-detail__section hc-surface">
            <summary class="vehicle-detail__section-summary">
              <span class="vehicle-detail__section-title">Completed recalls</span>
              {#if completedRecalls.length > 0}
                <span class="vehicle-detail__section-count">{completedRecalls.length}</span>
              {/if}
              <span aria-hidden="true" class="vehicle-detail__chevron"></span>
            </summary>
            <div class="vehicle-detail__section-body">
              {#if completedRecalls.length === 0}
                <div class="hc-empty-state hc-empty-state--compact">
                  <span class="hc-empty-state__icon" aria-hidden="true">
                    <svg fill="none" height="22" viewBox="0 0 24 24" width="22">
                      <path
                        d="M12 7v5l3 2"
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="1.7"
                      />
                      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                    </svg>
                  </span>
                  <p class="hc-empty-state__title">Nothing logged yet</p>
                  <p class="hc-empty-state__text">Recalls you resolve will appear here.</p>
                </div>
              {:else}
                <ul class="vehicle-detail__rows">
                  {#each completedRecalls as item (item.completedRecallId)}
                    <li>
                      <button
                        class="vehicle-detail__row-btn"
                        onclick={() => {
                          selectedCompletedRecall = item;
                          selectedUncompletedRecall = null;
                        }}
                        type="button"
                      >
                        <span class="vehicle-detail__row-main">
                          <span class="vehicle-detail__row-title">{item.component}</span>
                          <span class="vehicle-detail__row-meta">{item.completedDate}</span>
                        </span>
                        <span aria-hidden="true" class="vehicle-detail__row-arrow"></span>
                      </button>
                    </li>
                  {/each}
                </ul>
              {/if}
            </div>
          </details>
        </div>
      {/if}
    {/if}
  </div>

  {#if isMileageModalOpen && vehicle}
    <UpdateMileageModal
      currentMileage={vehicle.currentMileage}
      onclose={() => (isMileageModalOpen = false)}
      onupdated={onMileageUpdated}
      vin={vehicle.vin}
    />
  {/if}

  {#if isPhotoModalOpen && vehicle}
    <VehiclePhotoModal
      availableImageUrls={vehicle.availableImageUrls}
      onclose={() => (isPhotoModalOpen = false)}
      onupdated={onPhotoUpdated}
      selectedImageUrl={vehicle.selectedImageUrl}
      vin={vehicle.vin}
    />
  {/if}

  {#if isWarrantyModalOpen && vehicleWarranty}
    <WarrantyModal onclose={() => (isWarrantyModalOpen = false)} warranty={vehicleWarranty} />
  {/if}

  {#if isListingsModalOpen && vehicle}
    <VehicleListingsModal onclose={() => (isListingsModalOpen = false)} vin={vehicle.vin} />
  {/if}

  {#if isMaintenanceCostsModalOpen && vehicle}
    <MaintenanceCostsModal
      costs={miscMaintenanceCosts}
      onclose={() => (isMaintenanceCostsModalOpen = false)}
    />
  {/if}

  {#if selectedUpcomingMaintenance && vehicle}
    <MaintenanceDetailModal
      currentMileage={vehicle.currentMileage}
      isCompleted={false}
      onchanged={onMaintenanceChanged}
      onclose={closeMaintenanceModal}
      upcomingItem={selectedUpcomingMaintenance}
      vin={vehicle.vin}
    />
  {/if}

  {#if selectedCompletedMaintenance && vehicle}
    <MaintenanceDetailModal
      completedItem={selectedCompletedMaintenance}
      currentMileage={vehicle.currentMileage}
      isCompleted={true}
      onchanged={onMaintenanceChanged}
      onclose={closeMaintenanceModal}
      vin={vehicle.vin}
    />
  {/if}

  {#if selectedUncompletedRecall && vehicle}
    <RecallDetailModal
      isCompleted={false}
      onchanged={onRecallChanged}
      onclose={closeRecallModal}
      uncompletedItem={selectedUncompletedRecall}
      vin={vehicle.vin}
    />
  {/if}

  {#if selectedCompletedRecall && vehicle}
    <RecallDetailModal
      completedItem={selectedCompletedRecall}
      isCompleted={true}
      onchanged={onRecallChanged}
      onclose={closeRecallModal}
      vin={vehicle.vin}
    />
  {/if}
</section>

<style>
  .vehicle-detail__back {
    display: inline-block;
    margin-bottom: 1.5rem;
    color: var(--color-text-muted);
    text-decoration: none;
  }

  .vehicle-detail__back:hover {
    color: var(--color-text-on-dark);
  }

  .vehicle-detail__title {
    font-size: clamp(1.5rem, 3.5vw, 2rem);
    margin: 0 0 1rem;
  }

  .vehicle-detail__status--error {
    color: #f5a5a5;
  }

  .vehicle-detail__skeleton {
    display: grid;
    gap: 1.25rem;
  }

  .vehicle-detail__skeleton-title {
    height: 2rem;
    width: min(60%, 22rem);
    border-radius: var(--radius-pill);
  }

  .vehicle-detail__skeleton-image {
    aspect-ratio: 4 / 3;
    min-height: 220px;
    border-radius: 12px;
  }

  .vehicle-detail__skeleton-specs {
    height: 190px;
    border-radius: 12px;
  }

  .vehicle-detail__skeleton-btn {
    height: 2.75rem;
    border-radius: var(--radius-pill);
  }

  .vehicle-detail__skeleton-toggle {
    height: 2.75rem;
    border-radius: var(--radius-pill);
    margin-top: auto;
  }

  .vehicle-detail__skeleton-section {
    height: 3.5rem;
    border-radius: 14px;
  }

  .vehicle-detail__header {
    display: grid;
    grid-template-columns: minmax(0, 520px) minmax(0, 1fr);
    gap: 1.25rem;
    margin-bottom: 0.5rem;
    align-items: stretch;
  }

  @media (max-width: 768px) {
    .vehicle-detail__header {
      grid-template-columns: 1fr;
      align-items: start;
    }
  }

  .vehicle-detail__details-column {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    min-width: 0;
    min-height: 100%;
  }

  @media (max-width: 768px) {
    .vehicle-detail__details-column {
      min-height: 0;
    }
  }

  .vehicle-detail__specs {
    border-radius: var(--radius-md);
    padding: 0.85rem 1rem;
    width: 100%;
    box-sizing: border-box;
    flex-shrink: 0;
  }

  .vehicle-detail__dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 0.75rem;
    row-gap: 0.3rem;
    margin: 0;
    font-size: 0.8125rem;
    line-height: 1.35;
  }

  .vehicle-detail__dl > div {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 0.25rem 0.4rem;
    min-width: 0;
  }

  .vehicle-detail__dl-row--full {
    grid-column: 1 / -1;
  }

  .vehicle-detail__dl dt {
    font-size: 0.625rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--color-text-muted);
    flex-shrink: 0;
  }

  .vehicle-detail__dl dt::after {
    content: ':';
  }

  .vehicle-detail__dl dd {
    margin: 0;
    min-width: 0;
    word-break: break-word;
  }

  .vehicle-detail__mileage {
    margin: 0 0 0.5rem;
    font-size: 0.9375rem;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
  }

  .vehicle-detail__mileage-value {
    font-weight: 700;
    font-size: 1.15rem;
  }

  .vehicle-detail__mileage-btn {
    border: 1px solid rgba(230, 240, 251, 0.25);
    background: transparent;
    color: var(--color-text-on-dark);
    border-radius: 999px;
    padding: 0.2rem 0.65rem;
    cursor: pointer;
    font-size: 0.75rem;
  }

  .vehicle-detail__mileage-btn:hover {
    border-color: var(--color-accent);
    color: var(--color-accent);
  }

  .vehicle-detail__specs-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-top: 0.75rem;
  }

  .vehicle-detail__specs-actions .vehicle-detail__manual-btn-wrap {
    margin-top: 0;
  }

  .vehicle-detail__manual-btn-wrap {
    display: block;
    margin-top: 0.75rem;
  }

  .vehicle-detail__manual-btn {
    border: 1px solid rgba(230, 240, 251, 0.25);
    background: transparent;
    color: var(--color-text-on-dark);
    border-radius: 999px;
    padding: 0.35rem 0.85rem;
    cursor: pointer;
    font-size: 0.8125rem;
  }

  .vehicle-detail__manual-btn:hover:not(:disabled) {
    border-color: var(--color-accent);
    color: var(--color-accent);
  }

  .vehicle-detail__manual-btn:disabled,
  .vehicle-detail__manual-btn--unavailable {
    cursor: not-allowed;
    color: var(--color-text-muted);
    border-color: rgba(230, 240, 251, 0.15);
    opacity: 0.65;
  }

  .vehicle-detail__costs-btn-wrap {
    display: block;
    width: 100%;
  }

  .vehicle-detail__costs-btn {
    width: 100%;
    border: 1px solid rgba(230, 240, 251, 0.25);
    background: transparent;
    color: var(--color-text-on-dark);
    border-radius: 999px;
    padding: 0.7rem 1.5rem;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 600;
    flex-shrink: 0;
  }

  .vehicle-detail__costs-btn:hover:not(:disabled) {
    border-color: var(--color-accent);
    color: var(--color-accent);
  }

  .vehicle-detail__costs-btn:disabled,
  .vehicle-detail__costs-btn--unavailable {
    cursor: not-allowed;
    color: var(--color-text-muted);
    border-color: rgba(230, 240, 251, 0.15);
    opacity: 0.65;
  }

  .vehicle-detail__image-wrap {
    position: relative;
    border-radius: 12px;
    overflow: hidden;
    aspect-ratio: 4 / 3;
    min-height: 220px;
    min-width: 0;
    max-width: 100%;
  }

  @media (min-width: 769px) {
    .vehicle-detail__image-wrap {
      min-height: 280px;
    }
  }

  .vehicle-detail__image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .vehicle-detail__image-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--color-text-muted);
  }

  .vehicle-detail__photo-btn {
    position: absolute;
    top: 0.65rem;
    left: 0.65rem;
    z-index: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2.25rem;
    height: 2.25rem;
    border: none;
    border-radius: 999px;
    background: rgba(7, 20, 43, 0.55);
    color: var(--color-text-on-dark);
    cursor: pointer;
    opacity: 0;
    transition:
      opacity 160ms ease,
      background-color 160ms ease;
  }

  .vehicle-detail__image-wrap:hover .vehicle-detail__photo-btn,
  .vehicle-detail__image-wrap:focus-within .vehicle-detail__photo-btn,
  .vehicle-detail__photo-btn:focus-visible {
    opacity: 1;
  }

  .vehicle-detail__photo-btn:hover {
    background: rgba(7, 20, 43, 0.75);
  }

  @media (hover: none) {
    .vehicle-detail__photo-btn {
      opacity: 1;
    }
  }

  .vehicle-detail__photo-icon {
    display: block;
  }

  .vehicle-detail__toggle {
    position: relative;
    display: flex;
    width: 100%;
    margin-top: auto;
    flex-shrink: 0;
    padding: 0.25rem;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-pill);
    background: var(--color-surface-glass);
  }

  .vehicle-detail__toggle::before {
    content: '';
    position: absolute;
    top: 0.25rem;
    bottom: 0.25rem;
    left: 0.25rem;
    width: calc((100% - 0.5rem) / 2);
    border-radius: var(--radius-pill);
    background-image: var(--gradient-accent);
    box-shadow: var(--shadow-accent);
    transition: transform var(--dur-base) var(--ease-emphasized);
  }

  .vehicle-detail__toggle[data-active='recalls']::before {
    transform: translateX(100%);
  }

  @media (max-width: 768px) {
    .vehicle-detail__toggle {
      margin-top: 0;
    }
  }

  .vehicle-detail__toggle-btn {
    position: relative;
    z-index: 1;
    flex: 1;
    border: none;
    background: transparent;
    color: var(--color-text-muted);
    padding: 0.6rem 1.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: color var(--dur-base) var(--ease-standard);
  }

  .vehicle-detail__toggle-btn--active {
    color: var(--color-text-on-dark);
  }

  @media (prefers-reduced-motion: reduce) {
    .vehicle-detail__photo-btn,
    .vehicle-detail__toggle::before,
    .vehicle-detail__toggle-btn {
      transition: none;
    }
  }

  .vehicle-detail__panel {
    interpolate-size: allow-keywords;
    display: grid;
    gap: 0.75rem;
  }

  .vehicle-detail__section {
    border-radius: 14px;
    overflow: hidden;
  }

  .vehicle-detail__section-summary {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.95rem 1.15rem;
    cursor: pointer;
    list-style: none;
    user-select: none;
    transition: background-color 160ms ease;
  }

  .vehicle-detail__section-summary::-webkit-details-marker {
    display: none;
  }

  .vehicle-detail__section-summary:hover {
    background: rgba(255, 255, 255, 0.045);
  }

  .vehicle-detail__section-title {
    font-weight: 600;
    font-size: 1.0625rem;
  }

  .vehicle-detail__section-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 1.5rem;
    height: 1.5rem;
    padding: 0 0.5rem;
    border-radius: 999px;
    border: 1px solid var(--color-border-subtle);
    background: var(--color-accent-tint);
    color: var(--color-text-muted);
    font-size: 0.8125rem;
    font-weight: 600;
    line-height: 1;
  }

  .vehicle-detail__section-count--alert {
    border-color: var(--color-alert-error-border);
    background: var(--color-alert-error-bg);
    color: var(--color-alert-error-text);
  }

  .vehicle-detail__chevron {
    margin-left: auto;
    flex-shrink: 0;
    width: 0.6rem;
    height: 0.6rem;
    margin-top: -0.2rem;
    border-right: 2px solid var(--color-text-muted);
    border-bottom: 2px solid var(--color-text-muted);
    transform: rotate(45deg);
    transition:
      transform 220ms ease,
      border-color 160ms ease;
  }

  .vehicle-detail__section[open] > .vehicle-detail__section-summary .vehicle-detail__chevron {
    transform: rotate(-135deg);
    margin-top: 0.15rem;
  }

  .vehicle-detail__section-summary:hover .vehicle-detail__chevron {
    border-color: var(--color-text-on-dark);
  }

  .vehicle-detail__section-body {
    padding: 0 1.15rem 1.15rem;
  }

  @supports (interpolate-size: allow-keywords) {
    .vehicle-detail__section::details-content {
      block-size: 0;
      overflow: clip;
      opacity: 0;
      transition:
        block-size 260ms ease,
        opacity 200ms ease,
        content-visibility 260ms allow-discrete;
    }

    .vehicle-detail__section[open]::details-content {
      block-size: calc-size(auto, size);
      opacity: 1;
    }
  }

  .vehicle-detail__rows {
    list-style: none;
    margin: 0;
    padding: 0;
    border: 1px solid var(--color-border-subtle);
    border-radius: 10px;
    overflow: hidden;
  }

  .vehicle-detail__rows > li + li {
    border-top: 1px solid var(--color-border-subtle);
  }

  .vehicle-detail__row-btn {
    width: 100%;
    text-align: left;
    background: transparent;
    color: inherit;
    border: none;
    padding: 0.8rem 1rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    transition: background-color 140ms ease;
  }

  .vehicle-detail__row-main {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    flex: 1;
    min-width: 0;
  }

  .vehicle-detail__row-title {
    font-weight: 600;
  }

  .vehicle-detail__row-meta {
    font-size: 0.8125rem;
    color: var(--color-text-muted);
  }

  .vehicle-detail__row-arrow {
    flex-shrink: 0;
    width: 0.5rem;
    height: 0.5rem;
    border-top: 2px solid var(--color-text-muted);
    border-right: 2px solid var(--color-text-muted);
    transform: rotate(45deg);
    transition:
      transform 160ms ease,
      border-color 140ms ease;
  }

  .vehicle-detail__row-btn:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .vehicle-detail__row-btn:hover .vehicle-detail__row-arrow {
    border-color: var(--color-accent);
    transform: translateX(2px) rotate(45deg);
  }

  @media (prefers-reduced-motion: reduce) {
    .vehicle-detail__section-summary,
    .vehicle-detail__chevron,
    .vehicle-detail__row-btn,
    .vehicle-detail__row-arrow,
    .vehicle-detail__section::details-content {
      transition: none;
    }
  }

  .vehicle-detail__inspect-filter {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    margin: 0.25rem 0 1rem;
    font-size: 0.875rem;
    color: var(--color-text-muted);
    cursor: pointer;
    user-select: none;
  }

  .vehicle-detail__inspect-filter input {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
  }

  .vehicle-detail__inspect-track {
    position: relative;
    flex-shrink: 0;
    width: 2.1rem;
    height: 1.15rem;
    border-radius: 999px;
    border: 1px solid var(--color-border-strong);
    background: rgba(255, 255, 255, 0.06);
    transition:
      background-color 160ms ease,
      border-color 160ms ease;
  }

  .vehicle-detail__inspect-track::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 0.15rem;
    width: 0.85rem;
    height: 0.85rem;
    border-radius: 50%;
    background: var(--color-text-muted);
    transform: translateY(-50%);
    transition:
      transform 180ms ease,
      background-color 160ms ease;
  }

  .vehicle-detail__inspect-filter input:checked + .vehicle-detail__inspect-track {
    background: var(--color-accent);
    border-color: var(--color-accent);
  }

  .vehicle-detail__inspect-filter input:checked + .vehicle-detail__inspect-track::after {
    transform: translate(0.95rem, -50%);
    background: var(--color-text-on-dark);
  }

  .vehicle-detail__inspect-filter input:focus-visible + .vehicle-detail__inspect-track {
    outline: 2px solid var(--color-blue-300);
    outline-offset: 2px;
  }

  .vehicle-detail__mileage-groups {
    display: grid;
    gap: 1rem;
  }

  .vehicle-detail__mileage-group {
    border: 1px solid var(--color-border-subtle);
    border-radius: 10px;
    overflow: hidden;
  }

  .vehicle-detail__mileage-heading {
    margin: 0;
    padding: 0.6rem 0.95rem;
    font-size: 0.8125rem;
    font-weight: 600;
    letter-spacing: 0.02em;
    color: var(--color-blue-200);
    background: rgba(255, 255, 255, 0.035);
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .vehicle-detail__list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .vehicle-detail__maint-item {
    list-style: none;
  }

  .vehicle-detail__maint-item + .vehicle-detail__maint-item {
    border-top: 1px solid var(--color-border-subtle);
  }

  .vehicle-detail__item-head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.7rem 0.95rem;
  }

  .vehicle-detail__maint-summary-main {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    flex: 1;
    min-width: 0;
  }

  .vehicle-detail__maint-desc {
    min-width: 0;
    word-break: break-word;
  }

  .vehicle-detail__maint-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.4rem;
  }

  .vehicle-detail__maint-badge {
    font-size: 0.6875rem;
    font-weight: 600;
    letter-spacing: 0.02em;
    text-transform: uppercase;
    color: var(--color-accent);
    border: 1px solid var(--color-border-strong);
    border-radius: 999px;
    padding: 0.1rem 0.45rem;
  }

  .vehicle-detail__maint-labor {
    font-size: 0.8125rem;
    color: var(--color-text-muted);
  }

  .vehicle-detail__maint-action {
    flex-shrink: 0;
    border: 1px solid var(--color-border-strong);
    background: transparent;
    color: var(--color-accent);
    border-radius: 999px;
    padding: 0.35rem 0.8rem;
    font-size: 0.8125rem;
    font-weight: 600;
    cursor: pointer;
    transition:
      background-color 140ms ease,
      border-color 140ms ease;
  }

  .vehicle-detail__maint-action:hover {
    border-color: var(--color-accent);
    background: var(--color-accent-tint);
  }

  .vehicle-detail__mileage-summary {
    display: grid;
    gap: 0.35rem;
    margin: 0;
    padding: 0.7rem 0.95rem;
    border-top: 1px solid var(--color-border-subtle);
    background: rgba(255, 255, 255, 0.02);
    font-size: 0.875rem;
  }

  .vehicle-detail__mileage-summary > div {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
  }

  .vehicle-detail__mileage-summary dt {
    color: var(--color-text-muted);
  }

  .vehicle-detail__mileage-summary dd {
    margin: 0;
  }

  .vehicle-detail__mileage-summary-total {
    margin-top: 0.15rem;
    padding-top: 0.4rem;
    border-top: 1px solid var(--color-border-subtle);
    font-weight: 600;
    color: inherit;
  }

  @media (prefers-reduced-motion: reduce) {
    .vehicle-detail__inspect-track,
    .vehicle-detail__inspect-track::after,
    .vehicle-detail__maint-action {
      transition: none;
    }
  }
</style>
