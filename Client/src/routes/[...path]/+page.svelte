<script lang="ts">
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  onMount(() => {
    goto('/', { replaceState: true });
  });
</script>

<svelte:head>
  <title>Honest Car</title>
</svelte:head>
