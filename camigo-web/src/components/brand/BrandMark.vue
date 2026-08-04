<script setup lang="ts">
withDefaults(defineProps<{ size?: number }>(), { size: 68 })
</script>

<template>
  <svg
    class="brand-mark"
    :width="size"
    :height="size"
    viewBox="0 0 72 72"
    role="img"
    aria-label="校速达"
  >
    <rect width="72" height="72" rx="18" fill="currentColor" />
    <path d="M15 25h18M12 31h16" stroke="white" stroke-width="4" stroke-linecap="round" />
    <path d="M26 42V31a10 10 0 0 1 20 0v6" fill="none" stroke="white" stroke-width="5" stroke-linecap="round" />
    <rect x="18" y="35" width="34" height="23" rx="7" fill="white" />
    <path d="m36 43 13-6 11 6-13 7-11-7Z" fill="#dbeeff" />
    <path d="m36 43 11 7v13l-11-6V43Zm24 0-13 7v13l13-7V43Z" fill="white" stroke="#0a416f" stroke-width="2" stroke-linejoin="round" />
  </svg>
</template>

<style scoped>
.brand-mark {
  display: block;
  color: #073d69;
  filter: drop-shadow(0 12px 24px rgb(3 35 63 / 24%));
}
</style>
