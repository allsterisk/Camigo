<script setup lang="ts">
interface SummaryItem {
  label: string
  value: string | number
  hint: string
  tone: 'teal' | 'blue' | 'green'
}

defineProps<{ items: readonly SummaryItem[] }>()
</script>

<template>
  <section class="courier-summary" aria-label="配送工作概览">
    <article
      v-for="item in items"
      :key="item.label"
      class="courier-summary__card"
      :class="`courier-summary__card--${item.tone}`"
    >
      <span class="courier-summary__label">{{ item.label }}</span>
      <strong class="courier-summary__value">{{ item.value }}</strong>
      <small class="courier-summary__hint">{{ item.hint }}</small>
    </article>
  </section>
</template>

<style scoped>
.courier-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-bottom: 26px; }
.courier-summary__card { position: relative; overflow: hidden; min-height: 154px; padding: 24px; border: 1px solid #dfece9; border-radius: 18px; background: #fff; box-shadow: 0 12px 32px rgb(12 71 68 / 6%); }
.courier-summary__card::after { position: absolute; width: 90px; height: 90px; right: -26px; top: -28px; border-radius: 50%; content: ''; background: currentColor; opacity: .08; }
.courier-summary__card--teal { color: #087f73; }
.courier-summary__card--blue { color: #1976a8; }
.courier-summary__card--green { color: #279866; }
.courier-summary__label, .courier-summary__hint { display: block; }
.courier-summary__label { color: #637c7b; font-size: 13px; }
.courier-summary__value { display: block; margin: 14px 0 8px; color: currentColor; font-size: 38px; line-height: 1; }
.courier-summary__hint { color: #91a4a2; font-size: 11px; }

@media (max-width: 760px) {
  .courier-summary { grid-template-columns: 1fr; }
  .courier-summary__card { min-height: 128px; }
}
</style>
