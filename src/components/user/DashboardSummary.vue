<script setup lang="ts">
interface SummaryItem {
  label: string
  value: number
  hint: string
  tone: 'blue' | 'amber' | 'green'
}

defineProps<{ items: SummaryItem[] }>()
</script>

<template>
  <section class="dashboard-summary" aria-label="快递概览">
    <article v-for="item in items" :key="item.label" class="dashboard-summary__card" :class="`dashboard-summary__card--${item.tone}`">
      <span class="dashboard-summary__label">{{ item.label }}</span>
      <strong class="dashboard-summary__value">{{ item.value }}</strong>
      <small class="dashboard-summary__hint">{{ item.hint }}</small>
    </article>
  </section>
</template>

<style scoped>
.dashboard-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-bottom: 26px; }
.dashboard-summary__card { position: relative; overflow: hidden; min-height: 154px; padding: 24px; border: 1px solid #e0eaf2; border-radius: 18px; background: #fff; box-shadow: 0 12px 32px rgb(15 59 92 / 6%); }
.dashboard-summary__card::after { position: absolute; width: 90px; height: 90px; right: -26px; top: -28px; border-radius: 50%; content: ''; background: currentColor; opacity: .08; }
.dashboard-summary__card--blue { color: #0c76ad; }
.dashboard-summary__card--amber { color: #d8871c; }
.dashboard-summary__card--green { color: #219766; }
.dashboard-summary__label, .dashboard-summary__hint { display: block; }
.dashboard-summary__label { color: #637c91; font-size: 13px; }
.dashboard-summary__value { display: block; margin: 14px 0 8px; color: currentColor; font-size: 38px; line-height: 1; }
.dashboard-summary__hint { color: #91a1ae; font-size: 11px; }

@media (max-width: 760px) {
  .dashboard-summary { grid-template-columns: 1fr; }
  .dashboard-summary__card { min-height: 128px; }
}
</style>
