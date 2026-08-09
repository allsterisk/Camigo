<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts/core'
import BaseChart from '@/components/admin/BaseChart.vue'
import type { CourierEfficiency } from '@/types/statistics'

const props = defineProps<{ data: readonly CourierEfficiency[]; loading: boolean }>()
const ranking = computed(() => [...props.data].sort((a, b) => a.completedOrders - b.completedOrders).slice(-8))
const option = computed<EChartsOption>(() => ({
  animationDuration: 480, animationEasing: 'cubicOut',
  aria: { enabled: true, description: '配送员已完成订单数量排行' },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, renderMode: 'richText' },
  grid: { left: 8, right: 28, top: 12, bottom: 10, containLabel: true },
  xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf2f5' } }, axisLabel: { color: '#7890a1' } },
  yAxis: { type: 'category', data: ranking.value.map((item) => item.realName), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#4f687a', width: 72, overflow: 'truncate' } },
  series: [{ name: '完成订单', type: 'bar', barWidth: 14, data: ranking.value.map((item) => item.completedOrders), itemStyle: { color: '#1678a5', borderRadius: [0, 4, 4, 0] }, label: { show: true, position: 'right', color: '#345a72' } }],
}))
</script>
<template>
  <section class="chart-panel"><header class="chart-panel__header"><div><h2>配送效率排行</h2><p>按完成订单量展示前八名配送员</p></div><span>TOP 8</span></header><BaseChart :option="option" :loading="loading" :empty="ranking.length === 0" aria-label="配送员完成订单数量排行柱状图" /></section>
</template>
<style scoped src="./chart-panel.css"></style>
