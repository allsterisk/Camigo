<script setup lang="ts">
import { computed } from 'vue'

import logoSource from '@/assets/brand/camigo-logo-source.png'

const props = withDefaults(defineProps<{
  size?: number
  seamless?: boolean
}>(), {
  size: 68,
  seamless: false,
})

const markStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size * 0.935}px`,
}))

const sourceStyle = computed(() => ({
  top: `${props.size * (props.seamless ? -0.302 : -0.264)}px`,
  left: `${props.size * (props.seamless ? -0.325 : -0.283)}px`,
  width: `${props.size * (props.seamless ? 1.635 : 1.545)}px`,
}))
</script>

<template>
  <span
    class="brand-mark"
    :style="markStyle"
    role="img"
    aria-label="校速达"
  >
    <img
      class="brand-mark__source"
      :style="sourceStyle"
      :src="logoSource"
      alt=""
      aria-hidden="true"
    />
  </span>
</template>

<style scoped>
.brand-mark {
  position: relative;
  overflow: hidden;
  display: block;
  flex-shrink: 0;
  border-radius: 20%;
  filter: drop-shadow(0 10px 20px rgb(3 35 63 / 20%));
}

.brand-mark__source {
  position: absolute;
  max-width: none;
  height: auto;
  user-select: none;
  pointer-events: none;
}
</style>
