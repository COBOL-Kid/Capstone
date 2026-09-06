<script lang="ts">
  import { updateSelectedPhoto } from '$lib/services/vin';

  import Dialog from './Dialog.svelte';

  interface Props {
    vin: string;
    availableImageUrls: string[];
    selectedImageUrl: string;
    onclose: () => void;
    onupdated: (selectedImageUrl: string) => void;
  }

  let { vin, availableImageUrls, selectedImageUrl, onclose, onupdated }: Props = $props();

  let savingUrl = $state<string | null>(null);
  let serverError = $state<string | null>(null);

  const busy = $derived(savingUrl !== null);

  async function selectPhoto(url: string): Promise<void> {
    if (savingUrl) {
      return;
    }
    if (url === selectedImageUrl) {
      onclose();
      return;
    }
    serverError = null;
    savingUrl = url;
    try {
      await updateSelectedPhoto(vin, { selectedImageUrl: url });
      onupdated(url);
      onclose();
    } catch {
      serverError = 'Unable to update photo. Please try again.';
    } finally {
      savingUrl = null;
    }
  }
</script>

<Dialog
  {busy}
  closeLabel="Close vehicle photos dialog"
  labelledBy="vehicle-photo-modal-title"
  maxWidth="36rem"
  {onclose}
>
  {#snippet header()}
    <header class="hc-dialog__header">
      <h2 id="vehicle-photo-modal-title">Vehicle photos</h2>
      <p class="vehicle-photo-modal__subtitle">Select a photo to use as the default image.</p>
    </header>
  {/snippet}

  {#snippet children()}
    <div class="vehicle-photo-modal__body">
      {#if availableImageUrls.length === 0}
        <p class="vehicle-photo-modal__status">No photos available.</p>
      {:else}
        <div aria-label="Available vehicle photos" class="vehicle-photo-modal__grid" role="group">
          {#each availableImageUrls as url, index (url)}
            <button
              aria-label="Select photo {index + 1}"
              aria-pressed={url === selectedImageUrl}
              class="vehicle-photo-modal__tile"
              class:vehicle-photo-modal__tile--selected={url === selectedImageUrl}
              class:vehicle-photo-modal__tile--saving={savingUrl === url}
              disabled={savingUrl !== null}
              onclick={() => selectPhoto(url)}
              type="button"
            >
              <img alt="Vehicle photo {index + 1}" class="vehicle-photo-modal__image" src={url} />
              {#if url === selectedImageUrl}
                <span aria-hidden="true" class="vehicle-photo-modal__check">✓</span>
              {/if}
              {#if savingUrl === url}
                <span aria-hidden="true" class="vehicle-photo-modal__saving">Saving...</span>
              {/if}
            </button>
          {/each}
        </div>
      {/if}
    </div>

    {#if serverError}
      <div class="hc-alert hc-alert--error" role="alert">
        <p>{serverError}</p>
      </div>
    {/if}
  {/snippet}
</Dialog>

<style>
  .vehicle-photo-modal__subtitle {
    margin: 0.5rem 0 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .vehicle-photo-modal__status {
    margin: 0;
    color: var(--color-text-muted);
    text-align: center;
  }

  .vehicle-photo-modal__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
    gap: 0.75rem;
  }

  .vehicle-photo-modal__tile {
    position: relative;
    padding: 0;
    border: 2px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    overflow: hidden;
    background: var(--color-surface-glass);
    cursor: pointer;
    transition: border-color var(--dur-fast) var(--ease-standard);
  }

  .vehicle-photo-modal__tile:hover {
    border-color: var(--color-blue-300);
  }

  .vehicle-photo-modal__tile--selected {
    border-color: var(--color-accent);
  }

  .vehicle-photo-modal__tile--saving {
    opacity: 0.7;
  }

  .vehicle-photo-modal__tile:disabled {
    cursor: progress;
  }

  .vehicle-photo-modal__image {
    display: block;
    width: 100%;
    aspect-ratio: 4 / 3;
    object-fit: cover;
  }

  .vehicle-photo-modal__check {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    display: grid;
    place-items: center;
    width: 1.75rem;
    height: 1.75rem;
    border-radius: 999px;
    background: var(--color-accent);
    color: #fff;
    font-weight: 700;
  }

  .vehicle-photo-modal__saving {
    position: absolute;
    inset: auto 0 0 0;
    padding: 0.35rem;
    background: rgba(7, 20, 43, 0.8);
    color: var(--color-text-on-dark);
    font-size: var(--text-xs);
    text-align: center;
  }
</style>
