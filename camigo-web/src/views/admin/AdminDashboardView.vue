<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import CourierRankingChart from '@/components/admin/CourierRankingChart.vue'
import DashboardMetricRail from '@/components/admin/DashboardMetricRail.vue'
import ParcelTrendChart from '@/components/admin/ParcelTrendChart.vue'
import PickupPeakChart from '@/components/admin/PickupPeakChart.vue'
import { useAdminDashboard } from '@/composables/useAdminDashboard'

const ranges = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 },
] as const

const route = useRoute()
const router = useRouter()
const {
  resources,
  parcelStatistics,
  courierEfficiency,
  pickupTimes,
  periodMetrics,
  loading,
  errorMessage,
  updatedAt,
  load,
} = useAdminDashboard()

const days = computed<number>({
  get: () => {
    const value = Number(Array.isArray(route.query.days) ? route.query.days[0] : route.query.days)
    return ranges.some((range) => range.value === value) ? value : 30
  },
  set: (value) => {
    void router.replace({ query: { ...route.query, days: value === 30 ? undefined : String(value) } })
  },
})

const updatedLabel = computed(() => {
  if (!updatedAt.value) return '等待首次同步'
  return `更新于 ${new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(updatedAt.value)}`
})

watch(days, (value) => load(value), { immediate: true })
</script>

<template>
  <main class="operations-dashboard">
    <header class="operations-dashboard__header">
      <div class="operations-dashboard__heading">
        <h1>校园配送运营总览</h1>
        <p>汇总快递流转、配送效率与取件高峰，为日常调度提供依据。</p>
        <span class="operations-dashboard__updated"><i />{{ updatedLabel }}</span>
      </div>
      <div class="operations-dashboard__actions">
        <el-radio-group v-model="days" size="small" aria-label="统计时间范围">
          <el-radio-button v-for="range in ranges" :key="range.value" :value="range.value">
            {{ range.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" type="primary" @click="load(days)">刷新数据</el-button>
      </div>
    </header>

    <el-alert
      v-if="errorMessage"
      class="operations-dashboard__error"
      :title="errorMessage"
      description="请检查后端服务与登录状态后重试。"
      type="error"
      :closable="false"
      show-icon
    />

    <DashboardMetricRail
      :resources="resources"
      :metrics="periodMetrics"
      :days="days"
      :loading="loading"
    />

    <section class="operations-dashboard__charts" aria-label="运营统计图表">
      <ParcelTrendChart
        class="operations-dashboard__trend"
        :data="parcelStatistics"
        :loading="loading"
      />
      <CourierRankingChart :data="courierEfficiency" :loading="loading" />
      <PickupPeakChart :data="pickupTimes" :loading="loading" />
    </section>
  </main>
</template>

<style scoped>
.operations-dashboard { color: #173f5c; }
.operations-dashboard__header { display: flex; align-items: flex-end; justify-content: space-between; gap: 28px; margin-bottom: 26px; }
.operations-dashboard__heading h1 { max-width: 720px; margin: 0; color: #0d3654; font-size: clamp(28px, 3.4vw, 42px); line-height: 1.08; letter-spacing: -.03em; text-wrap: balance; }
.operations-dashboard__heading p { max-width: 66ch; margin: 12px 0 0; color: #637c8e; font-size: 14px; line-height: 1.7; }
.operations-dashboard__updated { display: inline-flex; align-items: center; margin-top: 12px; color: #7890a0; font-size: 11px; }
.operations-dashboard__updated i { width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; background: #27a374; box-shadow: 0 3px 8px rgb(39 163 116 / 28%); }
.operations-dashboard__actions { display: flex; align-items: center; gap: 12px; padding-bottom: 3px; }
.operations-dashboard__error { margin-bottom: 18px; }
.operations-dashboard__charts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; margin-top: 20px; }
.operations-dashboard__trend { grid-column: 1 / -1; }
@media (max-width: 920px) { .operations-dashboard__header { align-items: flex-start; flex-direction: column; }.operations-dashboard__charts { grid-template-columns: 1fr; }.operations-dashboard__trend { grid-column: auto; } }
@media (max-width: 640px) { .operations-dashboard__actions { width: 100%; align-items: stretch; flex-direction: column; }.operations-dashboard__actions :deep(.el-radio-group) { display: grid; grid-template-columns: repeat(3, 1fr); }.operations-dashboard__actions :deep(.el-radio-button__inner) { width: 100%; }.operations-dashboard__heading h1 { font-size: 30px; } }
</style>
