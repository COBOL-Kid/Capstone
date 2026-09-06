<script lang="ts">
  import type { FieldErrorMessage } from '$lib/api/errors';

  interface Props {
    error: FieldErrorMessage;
  }

  let { error }: Props = $props();
</script>

<div class="hc-alert hc-alert--error" role="alert">
  <p>{error.message}</p>
  {#if error.fieldMessages.length > 0}
    <ul>
      {#each error.fieldMessages as fieldMessage (fieldMessage)}
        <li>{fieldMessage}</li>
      {/each}
    </ul>
  {/if}
</div>
