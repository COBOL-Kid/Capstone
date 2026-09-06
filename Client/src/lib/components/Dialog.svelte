<script lang="ts">
  import { onMount, type Snippet } from 'svelte';

  interface Props {
    labelledBy: string;
    /** When true, backdrop/Escape/close are blocked (submit in flight). */
    busy: boolean;
    onclose: () => void;
    children: Snippet;
    header?: Snippet;
    closeLabel: string;
    maxWidth?: string;
  }

  let { labelledBy, busy, onclose, children, header, closeLabel, maxWidth }: Props = $props();

  let dialogEl: HTMLDivElement | null = $state(null);

  onMount(() => {
    dialogEl?.focus();
  });

  function requestClose(): void {
    if (busy) {
      return;
    }
    onclose();
  }

  function handleKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      requestClose();
    }
  }
</script>

<svelte:window onkeydown={handleKeydown} />

<div class="hc-modal-host">
  <div class="hc-overlay-backdrop" aria-hidden="true" onclick={requestClose}></div>

  <div
    bind:this={dialogEl}
    aria-labelledby={labelledBy}
    aria-modal="true"
    class="hc-dialog"
    role="dialog"
    tabindex="-1"
    style={maxWidth ? `--hc-dialog-max-width: ${maxWidth}` : undefined}
  >
    <button
      aria-label={closeLabel}
      class="hc-dialog__close"
      disabled={busy}
      onclick={requestClose}
      type="button"
    >
      ×
    </button>

    {@render header?.()}
    {@render children()}
  </div>
</div>
