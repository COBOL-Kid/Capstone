<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { ApiError } from '$lib/api/client';
  import { isSafeHttpUrl } from '$lib/api/safe-url';
  import { authStore } from '$lib/stores/auth.svelte';
  import { toastStore } from '$lib/stores/toast.svelte';
  import { vehiclesStore } from '$lib/stores/vehicles.svelte';
  import { addVehicle, getUserVehicles } from '$lib/services/vin';
  import type { AuthErrorMessage } from '$lib/models/auth';
  import type {
    AddVinRequest,
    AddVinResponse,
    AddVinTrimSelectionRequiredResponse,
    VinErrorMessage,
  } from '$lib/models/vin';

  import AddVehicleModal from '$lib/components/AddVehicleModal.svelte';
  import DeleteVehicleModal from '$lib/components/DeleteVehicleModal.svelte';
  import EmailVerificationStep from '$lib/components/EmailVerificationStep.svelte';

  let isAddModalOpen = $state(false);
  let isOnboardingVehicle = $state(false);
  let addVehicleError = $state<VinErrorMessage | null>(null);
  let trimSelectionContext = $state<AddVinTrimSelectionRequiredResponse | null>(null);
  let vehicleToDelete = $state<string | null>(null);
  let isVerificationPanelOpen = $state(false);
  let isVerifyingEmail = $state(false);
  let isResendingVerification = $state(false);
  let verificationError = $state<AuthErrorMessage | null>(null);
  let isLoading = $state(true);
  let loadError = $state<string | null>(null);

  const isEmailVerificationRequired = $derived(authStore.account?.emailVerified === false);
  const canAddVehicle = $derived(!isEmailVerificationRequired && !isOnboardingVehicle);

  onMount(async () => {
    try {
      await authStore.getCurrentAccount();
    } catch {
      // Guard already validated the session; hydration failure renders empty states.
    }
    await loadVehicles();
  });

  $effect(() => {
    if (authStore.account?.emailVerified === true) {
      void loadVehicles();
    }
  });

  async function loadVehicles(): Promise<void> {
    if (authStore.account?.emailVerified !== true) {
      isLoading = false;
      return;
    }
    isLoading = true;
    loadError = null;
    try {
      vehiclesStore.setVehicles(await getUserVehicles());
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.status === 403 &&
        typeof error.body === 'string' &&
        error.body.includes('Email address must be verified')
      ) {
        loadError = null;
      } else {
        loadError = 'Failed to load vehicles.';
      }
    } finally {
      isLoading = false;
    }
  }

  function openVerificationPanel(): void {
    verificationError = null;
    isVerificationPanelOpen = true;
  }

  function closeVerificationPanel(): void {
    isVerificationPanelOpen = false;
    verificationError = null;
  }

  async function resendVerificationEmail(): Promise<void> {
    if (isResendingVerification || isVerifyingEmail) {
      return;
    }
    verificationError = null;
    isResendingVerification = true;
    try {
      await authStore.resendEmailVerification();
    } catch (error) {
      verificationError = error as AuthErrorMessage;
    } finally {
      isResendingVerification = false;
    }
  }

  async function submitVerificationCode(code: string): Promise<void> {
    if (isVerifyingEmail || isResendingVerification) {
      return;
    }
    verificationError = null;
    isVerifyingEmail = true;
    try {
      await authStore.verifyEmailCode(code);
      await authStore.getCurrentAccount({ forceRefresh: true });
      closeVerificationPanel();
    } catch (error) {
      verificationError = error as AuthErrorMessage;
    } finally {
      isVerifyingEmail = false;
    }
  }

  function openAddModal(): void {
    if (!canAddVehicle) {
      return;
    }
    addVehicleError = null;
    trimSelectionContext = null;
    isAddModalOpen = true;
  }

  function closeAddModal(): void {
    if (isOnboardingVehicle) {
      return;
    }
    isAddModalOpen = false;
    addVehicleError = null;
    trimSelectionContext = null;
  }

  async function onAddVehicleRequest(request: AddVinRequest): Promise<void> {
    addVehicleError = null;
    isOnboardingVehicle = true;
    try {
      const result = await addVehicle(request);
      if (result.kind === 'trimSelectionRequired') {
        trimSelectionContext = result.context;
        isAddModalOpen = true;
        return;
      }
      trimSelectionContext = null;
      onVehicleAdded(result.response);
    } catch (error) {
      addVehicleError = error as VinErrorMessage;
      isAddModalOpen = true;
    } finally {
      isOnboardingVehicle = false;
    }
  }

  async function onVehicleAdded(response: AddVinResponse): Promise<void> {
    isAddModalOpen = false;
    addVehicleError = null;
    trimSelectionContext = null;
    toastStore.success('Vehicle added to your garage.');
    await goto(`/vehicles/${response.vin}`);
  }

  function openDeleteModal(vin: string): void {
    if (isOnboardingVehicle) {
      return;
    }
    vehicleToDelete = vin;
  }

  function onVehicleDeleted(vin: string): void {
    vehiclesStore.removeVehicle(vin);
    toastStore.success('Vehicle removed.');
  }
</script>

<svelte:head>
  <title>Home · Honest Car</title>
</svelte:head>

<section aria-labelledby="home-title" class="home hc-page-shell">
  <div class="home__content hc-page-shell__inner">
    {#if isEmailVerificationRequired}
      <div class="verify-banner" role="status" aria-live="polite">
        <span class="verify-banner__rail" aria-hidden="true"></span>
        <div class="verify-banner__main">
          <span class="verify-banner__icon" aria-hidden="true">
            <svg fill="none" height="22" viewBox="0 0 24 24" width="22">
              <rect
                height="13"
                rx="2.5"
                stroke="currentColor"
                stroke-width="1.7"
                width="18"
                x="3"
                y="5.5"
              />
              <path
                d="m4 7 7.3 5.2a1.2 1.2 0 0 0 1.4 0L20 7"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.7"
              />
            </svg>
          </span>
          <div class="verify-banner__copy">
            <p class="verify-banner__title">Verify your email</p>
            <p class="verify-banner__text">
              {#if authStore.account?.email}
                We sent a 6-digit code to <strong>{authStore.account.email}</strong> — verify to start
                adding vehicles.
              {:else}
                We sent a 6-digit code to your inbox — verify to start adding vehicles.
              {/if}
            </p>
            <p class="verify-banner__hint">
              <svg fill="none" height="14" viewBox="0 0 24 24" width="14">
                <circle cx="12" cy="12" r="8.25" stroke="currentColor" stroke-width="1.7" />
                <path
                  d="M12 7.75V12l2.75 1.75"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.7"
                />
              </svg>
              Code expires in 5 minutes
            </p>
          </div>
          {#if !isVerificationPanelOpen}
            <button
              class="hc-btn hc-btn--primary verify-banner__cta"
              onclick={openVerificationPanel}
              type="button"
            >
              Enter code
            </button>
          {/if}
        </div>
        {#if isVerificationPanelOpen}
          <div class="verify-banner__form">
            <EmailVerificationStep
              description="Enter the 6-digit code from your inbox to verify your email."
              email={authStore.account?.email ?? ''}
              error={verificationError}
              isResending={isResendingVerification}
              isSubmitting={isVerifyingEmail}
              oncancel={closeVerificationPanel}
              onresend={resendVerificationEmail}
              onverify={submitVerificationCode}
              showCancel={true}
              submitLabel="Verify email"
            />
          </div>
        {/if}
      </div>
    {/if}

    <div class="home__header">
      <h1 class="home__title" id="home-title">My Vehicles</h1>
      {#if vehiclesStore.vehicles.length > 0}
        <button
          aria-label="Add new vehicle"
          class="home__add-button"
          disabled={!canAddVehicle}
          onclick={openAddModal}
          type="button"
        >
          +
        </button>
      {/if}
    </div>

    {#if isLoading}
      <span class="hc-sr-only" role="status">Loading vehicles…</span>
      <div aria-hidden="true" class="home__grid">
        {#each [0, 1, 2] as placeholder (placeholder)}
          <article class="vehicle-card vehicle-card--skeleton hc-surface">
            <div class="vehicle-card__image-container hc-skeleton"></div>
            <div class="vehicle-card__details">
              <div
                class="hc-skeleton hc-skeleton--text vehicle-card__skeleton-line vehicle-card__skeleton-line--title"
              ></div>
              <div
                class="hc-skeleton hc-skeleton--text vehicle-card__skeleton-line vehicle-card__skeleton-line--subtitle"
              ></div>
              <div
                class="hc-skeleton hc-skeleton--text vehicle-card__skeleton-line vehicle-card__skeleton-line--mileage"
              ></div>
            </div>
          </article>
        {/each}
      </div>
    {:else if loadError}
      <div class="home__error hc-state-message">{loadError}</div>
    {:else if vehiclesStore.vehicles.length === 0}
      <div class="home__empty hc-empty-state">
        <span class="hc-empty-state__icon" aria-hidden="true">
          <svg fill="none" height="26" viewBox="0 0 24 24" width="26">
            <path
              d="M5 16.5 6.2 11a3 3 0 0 1 2.9-2.3h5.8a3 3 0 0 1 2.9 2.3L19 16.5M5 16.5h14M5 16.5v2.2a.8.8 0 0 1-.8.8H4a1 1 0 0 1-1-1v-2h2Zm14 0v2.2a.8.8 0 0 0 .8.8h.2a1 1 0 0 0 1-1v-2h-2Z"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.6"
            />
            <circle cx="7.5" cy="16.5" r="1.1" fill="currentColor" />
            <circle cx="16.5" cy="16.5" r="1.1" fill="currentColor" />
          </svg>
        </span>
        <h2 class="hc-empty-state__title">No vehicles yet</h2>
        <p class="hc-empty-state__text">
          Add your first vehicle to track maintenance, recalls, warranty coverage, and more.
        </p>
        <button class="hc-btn hc-btn--primary" disabled={!canAddVehicle} onclick={openAddModal}>
          Add a vehicle
        </button>
      </div>
    {:else}
      <div class="home__grid">
        {#each vehiclesStore.vehicles as vehicle (vehicle.vin)}
          <article class="vehicle-card hc-surface">
            <a class="vehicle-card__link" href="/vehicles/{vehicle.vin}">
              <button
                aria-label="Delete vehicle"
                class="vehicle-card__delete"
                onclick={(event) => {
                  event.preventDefault();
                  event.stopPropagation();
                  openDeleteModal(vehicle.vin);
                }}
                type="button"
              >
                ×
              </button>
              <div class="vehicle-card__image-container">
                {#if isSafeHttpUrl(vehicle.selectedImageUrl)}
                  <img
                    alt="Photo of {vehicle.year} {vehicle.make} {vehicle.model}"
                    class="vehicle-card__image"
                    src={vehicle.selectedImageUrl}
                  />
                {:else}
                  <div class="vehicle-card__placeholder">No Image</div>
                {/if}
              </div>
              <div class="vehicle-card__details">
                <h3 class="vehicle-card__title">
                  {vehicle.year}
                  {vehicle.make}
                  {vehicle.model}
                </h3>
                <p class="vehicle-card__subtitle">{vehicle.trim}</p>
                <p class="vehicle-card__mileage">
                  {vehicle.currentMileage} miles
                </p>
              </div>
            </a>
          </article>
        {/each}
      </div>
    {/if}
  </div>

  {#if isAddModalOpen}
    <AddVehicleModal
      isSubmitting={isOnboardingVehicle}
      onclose={closeAddModal}
      onservererrorclear={() => (addVehicleError = null)}
      onsubmitrequest={onAddVehicleRequest}
      serverError={addVehicleError}
      {trimSelectionContext}
    />
  {/if}

  {#if vehicleToDelete}
    <DeleteVehicleModal
      onclose={() => (vehicleToDelete = null)}
      ondeleted={onVehicleDeleted}
      vin={vehicleToDelete}
    />
  {/if}
</section>

<style>
  .verify-banner {
    position: relative;
    margin-bottom: 1.5rem;
    padding: 1.25rem 1.5rem 1.25rem 1.75rem;
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-lg);
    background: var(--color-surface-glass);
    backdrop-filter: blur(8px);
    box-shadow: var(--shadow-md);
    overflow: hidden;
    animation: verify-banner-in var(--dur-slow) var(--ease-standard);
  }

  .verify-banner__rail {
    position: absolute;
    inset: 0 auto 0 0;
    width: 4px;
    background: var(--gradient-accent);
  }

  .verify-banner__main {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .verify-banner__icon {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2.75rem;
    height: 2.75rem;
    border-radius: 50%;
    background: var(--color-accent-tint);
    border: 1px solid var(--color-border-strong);
    color: var(--color-blue-200);
  }

  .verify-banner__copy {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
  }

  .verify-banner__title {
    margin: 0;
    font-size: var(--text-lg);
    font-weight: 700;
    color: var(--color-text-on-dark);
  }

  .verify-banner__text {
    margin: 0;
    font-size: var(--text-sm);
    line-height: 1.45;
    color: var(--color-text-muted);
  }

  .verify-banner__text strong {
    font-weight: 600;
    color: var(--color-blue-100);
  }

  .verify-banner__hint {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    margin: 0.35rem 0 0;
    font-size: var(--text-xs);
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--color-blue-300);
  }

  .verify-banner__cta {
    flex-shrink: 0;
    white-space: nowrap;
    min-width: auto;
  }

  .verify-banner__form {
    margin-top: 1.25rem;
    padding-top: 1.25rem;
    border-top: 1px solid var(--color-border-subtle);
    animation: verify-banner-expand var(--dur-base) var(--ease-standard);
  }

  @keyframes verify-banner-in {
    from {
      opacity: 0;
      transform: translateY(-8px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @keyframes verify-banner-expand {
    from {
      opacity: 0;
      transform: translateY(-4px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @media (max-width: 640px) {
    .verify-banner__main {
      flex-direction: column;
      align-items: flex-start;
    }

    .verify-banner__cta {
      width: 100%;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .verify-banner,
    .verify-banner__form {
      animation: none;
    }
  }

  .home__header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .home__title {
    font-size: clamp(2rem, 5vw, 3rem);
    margin: 0;
    letter-spacing: 0.01em;
  }

  .home__add-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2.5rem;
    height: 2.5rem;
    border-radius: 50%;
    background-color: var(--color-accent);
    color: var(--color-text-on-dark);
    font-size: 1.5rem;
    font-weight: bold;
    border: none;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(47, 111, 180, 0.3);
    transition:
      transform 150ms ease,
      background-color 150ms ease;
  }

  .home__add-button:hover {
    background-color: var(--color-accent-strong);
    transform: translateY(-2px);
  }

  .home__add-button:disabled {
    cursor: not-allowed;
    opacity: 0.55;
    transform: none;
    box-shadow: none;
  }

  .home__add-button:disabled:hover {
    background-color: var(--color-accent);
    transform: none;
  }

  .home__empty p {
    margin-bottom: 1.5rem;
  }

  .home__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 2rem;
  }

  .vehicle-card--skeleton {
    cursor: default;
  }

  .vehicle-card--skeleton .vehicle-card__image-container {
    border-radius: 0;
  }

  .vehicle-card__skeleton-line {
    height: 0.85rem;
  }

  .vehicle-card__skeleton-line--title {
    width: 70%;
    height: 1.1rem;
    margin-bottom: 0.5rem;
  }

  .vehicle-card__skeleton-line--subtitle {
    width: 45%;
    margin-bottom: 1.25rem;
  }

  .vehicle-card__skeleton-line--mileage {
    width: 35%;
    margin-top: auto;
  }

  .vehicle-card {
    position: relative;
    border-radius: var(--radius-lg);
    overflow: hidden;
    transition:
      transform var(--dur-base) var(--ease-standard),
      box-shadow var(--dur-base) var(--ease-standard),
      border-color var(--dur-base) var(--ease-standard);
    display: flex;
    flex-direction: column;
  }

  .vehicle-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
    border-color: var(--color-border-strong);
  }

  .vehicle-card__link {
    display: flex;
    flex-direction: column;
    flex: 1;
    color: inherit;
    text-decoration: none;
  }

  .vehicle-card__image-container {
    width: 100%;
    aspect-ratio: 16 / 9;
    background: rgba(0, 0, 0, 0.3);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  .vehicle-card__image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform var(--dur-slow) var(--ease-standard);
  }

  .vehicle-card:hover .vehicle-card__image {
    transform: scale(1.05);
  }

  .vehicle-card__placeholder {
    color: var(--color-text-muted);
    font-weight: 500;
  }

  .vehicle-card__details {
    padding: 1.25rem;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .vehicle-card__title {
    margin: 0 0 0.2rem;
    font-size: var(--text-xl);
    font-weight: 700;
    line-height: 1.2;
    color: var(--color-text-on-dark);
  }

  .vehicle-card__subtitle {
    margin: 0 0 1rem;
    font-size: var(--text-sm);
    color: var(--color-blue-100);
  }

  .vehicle-card__mileage {
    margin: auto 0 0;
    padding-top: 0.75rem;
    border-top: 1px solid var(--color-border-subtle);
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .vehicle-card__delete {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.6);
    color: #fff;
    border: 1px solid rgba(255, 255, 255, 0.2);
    font-size: 1.25rem;
    line-height: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    opacity: 0;
    transition:
      opacity 200ms ease,
      background 150ms ease;
    z-index: 10;
  }

  .vehicle-card:hover .vehicle-card__delete,
  .vehicle-card:focus-within .vehicle-card__delete {
    opacity: 1;
  }

  .vehicle-card__delete:hover {
    background: #a92a2a;
    border-color: #ff4d4d;
  }

  @media (hover: none) {
    .vehicle-card__delete {
      opacity: 1;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .vehicle-card,
    .vehicle-card__image,
    .vehicle-card__delete {
      transition: none;
    }
  }
</style>
