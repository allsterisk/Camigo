<script setup lang="ts">
import { onBeforeUnmount, onMounted, useTemplateRef, watch } from 'vue'
import { BarChart, LineChart } from 'echarts/charts'
import {
  AriaComponent,
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { init, use, type ECharts, type EChartsOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([
  AriaComponent,
  BarChart,
  CanvasRenderer,
  DatasetComponent,
  GridComponent,
  LegendComponent,
  LineChart,
  TooltipComponent,
])

const props = withDefaults(defineProps<{
  option: EChartsOption
  ariaLabel: string
  loading?: boolean
  empty?: boolean
}>(), {
  loading: false,
  empty: false,
})

const chartElement = useTemplateRef<HTMLDivElement>('chart')
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function renderChart() {
  if (!chart || props.empty) return
  chart.setOption(props.option, { notMerge: true })
}

onMounted(() => {
  if (!chartElement.value) return
  chart = init(chartElement.value, undefined, { renderer: 'canvas' })
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
  renderChart()
})

watch(() => props.option, renderChart, { deep: true })
watch(() => props.empty, (empty) => {
  if (!empty) renderChart()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  resizeObserver = null
  chart = null
})
</script>

<template>
  <div class="base-chart" :aria-busy="loading">
    <div
      ref="chart"
      class="base-chart__canvas"
      role="img"
      :aria-label="ariaLabel"
      :class="{ 'base-chart__canvas--hidden': loading || empty }"
    />
    <div v-if="loading" class="base-chart__loading" aria-label="图表数据加载中">
      <span /><span /><span /><span /><span />
    </div>
    <div v-else-if="empty" class="base-chart__empty">
      <strong>暂无统计数据</strong>
      <span>当前时间范围内还没有可展示的记录</span>
    </div>
  </div>
</template>

<style scoped>
.base-chart { position: relative; min-height: 310px; }
.base-chart__canvas { width: 100%; height: 310px; transition: opacity 180ms ease-out; }
.base-chart__canvas--hidden { opacity: 0; }
.base-chart__loading { position: absolute; inset: 24px 18px 32px 48px; display: flex; align-items: flex-end; gap: 8%; }
.base-chart__loading span { flex: 1; min-height: 22%; border-radius: 6px 6px 2px 2px; background: #e7eff5; animation: chart-pulse 1.2s ease-in-out infinite alternate; }
.base-chart__loading span:nth-child(2) { height: 64%; animation-delay: 90ms; }
.base-chart__loading span:nth-child(3) { height: 42%; animation-delay: 180ms; }
.base-chart__loading span:nth-child(4) { height: 82%; animation-delay: 270ms; }
.base-chart__loading span:nth-child(5) { height: 56%; animation-delay: 360ms; }
.base-chart__empty { position: absolute; inset: 0; display: grid; align-content: center; justify-items: center; color: #718697; text-align: center; }
.base-chart__empty strong { color: #294f6d; font-size: 15px; }
.base-chart__empty span { margin-top: 7px; font-size: 12px; }
@keyframes chart-pulse { from { opacity: .45; } to { opacity: 1; } }
@media (prefers-reduced-motion: reduce) { .base-chart__loading span { animation: none; } }
</style>
