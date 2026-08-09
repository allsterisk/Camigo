<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts/core'

import BaseChart from '@/components/admin/BaseChart.vue'
import type { DailyParcelStatistic } from '@/types/statistics'

const props = defineProps<{ data: readonly DailyParcelStatistic[]; loading: boolean }>()

const dailyRows = computed(() => {
  const grouped = new Map<string, { parcels: number; picked: number; exceptions: number }>()
  for (const item of props.data) {
    const current = grouped.get(item.statDate) ?? { parcels: 0, picked: 0, exceptions: 0 }
    current.parcels += item.parcelCount
    current.picked += item.pickedUpCount
    current.exceptions += item.exceptionCount
    grouped.set(item.statDate, current)
  }
  return [...grouped.entries()].sort(([left], [right]) => left.localeCompare(right))
})

const option = computed<EChartsOption>(() => ({
  animationDuration: 520,
  animationEasing: 'cubicOut',
  aria: { enabled: true, description: '每日快递到校、完成取件和异常记录趋势' },
  tooltip: { trigger: 'axis', renderMode: 'richText' },
  legend: { top: 0, right: 0, itemWidth: 18, itemHeight: 8, textStyle: { color: '#60798c' } },
  grid: { left: 10, right: 14, top: 48, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: dailyRows.value.map(([date]) => date.slice(5).replace('-', '/')),
    axisLine: { lineStyle: { color: '#dce7ee' } },
    axisTick: { show: false },
    axisLabel: { color: '#7890a1', hideOverlap: true },
  },
  yAxis: {
    type: 'value', minInterval: 1,
    splitLine: { lineStyle: { color: '#edf2f5' } },
    axisLabel: { color: '#7890a1' },
  },
  series: [
    { name: '到校快递', type: 'line', smooth: .32, symbol: 'circle', symbolSize: 6, showSymbol: false, lineStyle: { width: 3, color: '#0b6f9f' }, itemStyle: { color: '#0b6f9f' }, areaStyle: { color: 'rgba(11,111,159,.10)' }, data: dailyRows.value.map(([, row]) => row.parcels) },
    { name: '完成取件', type: 'line', smooth: .32, showSymbol: false, lineStyle: { width: 2, color: '#20a474' }, itemStyle: { color: '#20a474' }, data: dailyRows.value.map(([, row]) => row.picked) },
    { name: '异常记录', type: 'line', smooth: .25, showSymbol: false, lineStyle: { width: 2, color: '#d56b52' }, itemStyle: { color: '#d56b52' }, data: dailyRows.value.map(([, row]) => row.exceptions) },
  ],
}))
</script>

<template>
  <section class="chart-panel chart-panel--wide">
    <header class="chart-panel__header"><div><h2>快递流转趋势</h2><p>按日汇总到校、取件与异常变化</p></div><span>每日</span></header>
    <BaseChart :option="option" :loading="loading" :empty="dailyRows.length === 0" aria-label="每日快递流转趋势折线图" />
  </section>
</template>

<style scoped src="./chart-panel.css"></style>
