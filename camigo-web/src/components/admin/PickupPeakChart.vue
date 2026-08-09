<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts/core'
import BaseChart from '@/components/admin/BaseChart.vue'
import type { PopularPickupTime } from '@/types/statistics'

const props = defineProps<{ data: readonly PopularPickupTime[]; loading: boolean }>()
const hourlyValues = computed(() => {
  const counts = new Map(props.data.map((item) => [item.hourOfDay, item.pickupCount]))
  return Array.from({ length: 24 }, (_, hour) => counts.get(hour) ?? 0)
})
const peakHour = computed(() => hourlyValues.value.reduce((best, value, hour, values) => value > values[best] ? hour : best, 0))
const option = computed<EChartsOption>(() => ({
  animationDuration: 480, animationEasing: 'cubicOut',
  aria: { enabled: true, description: '一天二十四小时取件次数分布' },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, renderMode: 'richText' },
  grid: { left: 8, right: 10, top: 20, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: Array.from({ length: 24 }, (_, hour) => `${String(hour).padStart(2, '0')}:00`), axisTick: { show: false }, axisLine: { lineStyle: { color: '#dce7ee' } }, axisLabel: { color: '#7890a1', interval: 3 } },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf2f5' } }, axisLabel: { color: '#7890a1' } },
  series: [{ name: '取件次数', type: 'bar', barMaxWidth: 18, data: hourlyValues.value.map((value, hour) => ({ value, itemStyle: { color: hour === peakHour.value && value > 0 ? '#e08a4b' : '#5aa6c6', borderRadius: [4, 4, 0, 0] } })) }],
}))
</script>
<template>
  <section class="chart-panel"><header class="chart-panel__header"><div><h2>高峰时间分析</h2><p>按小时识别取件需求集中的时段</p></div><span v-if="data.length">峰值 {{ String(peakHour).padStart(2, '0') }}:00</span></header><BaseChart :option="option" :loading="loading" :empty="data.length === 0" aria-label="二十四小时取件高峰分布柱状图" /></section>
</template>
<style scoped src="./chart-panel.css"></style>
