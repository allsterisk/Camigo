<script setup lang="ts">
import { computed } from 'vue'

interface ResourceSummary { users: number; parcels: number; couriers: number; lockers: number }
interface PeriodMetrics { parcels: number; picked: number; exceptions: number; pickupRate: number; activeCouriers: number }

const props = defineProps<{
  resources: ResourceSummary
  metrics: PeriodMetrics
  days: number
  loading: boolean
}>()

const operationalMetrics = computed(() => [
  { label: `${props.days} 天快递量`, value: props.metrics.parcels.toLocaleString('zh-CN'), suffix: '件' },
  { label: '完成取件', value: props.metrics.picked.toLocaleString('zh-CN'), suffix: '件' },
  { label: '周期取件率', value: props.metrics.pickupRate.toFixed(1), suffix: '%' },
  { label: '异常记录', value: props.metrics.exceptions.toLocaleString('zh-CN'), suffix: '条' },
])

const resourceLinks = computed(() => [
  { label: '平台用户', value: props.resources.users, path: '/admin/users' },
  { label: '全部快递', value: props.resources.parcels, path: '/admin/parcels' },
  { label: '配送员', value: props.resources.couriers, path: '/admin/couriers' },
  { label: '快递柜', value: props.resources.lockers, path: '/admin/lockers' },
])
</script>

<template>
  <section class="metric-rail" aria-label="运营核心指标">
    <div class="metric-rail__operations">
      <div v-for="metric in operationalMetrics" :key="metric.label" class="metric-rail__metric">
        <span>{{ metric.label }}</span>
        <strong :class="{ 'metric-rail__value--loading': loading }">
          {{ loading ? '—' : metric.value }}<small>{{ metric.suffix }}</small>
        </strong>
      </div>
    </div>
    <nav class="metric-rail__resources" aria-label="管理资源快捷入口">
      <RouterLink v-for="resource in resourceLinks" :key="resource.path" :to="resource.path">
        <span>{{ resource.label }}</span>
        <strong>{{ loading ? '—' : resource.value.toLocaleString('zh-CN') }}</strong>
      </RouterLink>
    </nav>
  </section>
</template>

<style scoped>
.metric-rail { display: grid; grid-template-columns: minmax(0, 2fr) minmax(310px, 1fr); overflow: hidden; border: 1px solid #dbe8f0; border-radius: 16px; background: #fff; box-shadow: 0 16px 38px rgb(13 65 99 / 7%); }
.metric-rail__operations { display: grid; grid-template-columns: repeat(4, 1fr); }
.metric-rail__metric { min-width: 0; padding: 22px 24px; border-right: 1px solid #e5edf2; }
.metric-rail__metric span { display: block; color: #657d90; font-size: 12px; }
.metric-rail__metric strong { display: block; margin-top: 9px; color: #123f60; font-size: clamp(25px, 2.2vw, 34px); letter-spacing: -.025em; }
.metric-rail__metric small { margin-left: 5px; color: #72899a; font-size: 11px; font-weight: 600; }
.metric-rail__value--loading { color: #9eb0bd !important; }
.metric-rail__resources { display: grid; grid-template-columns: repeat(2, 1fr); background: #0b3654; }
.metric-rail__resources a { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 62px; padding: 14px 18px; color: #c6d9e6; text-decoration: none; transition: background 160ms ease-out; }
.metric-rail__resources a:nth-child(odd) { border-right: 1px solid rgb(255 255 255 / 10%); }
.metric-rail__resources a:nth-child(-n+2) { border-bottom: 1px solid rgb(255 255 255 / 10%); }
.metric-rail__resources a:hover,.metric-rail__resources a:focus-visible { background: #124c70; outline: none; }
.metric-rail__resources span { font-size: 12px; }
.metric-rail__resources strong { color: #fff; font-size: 18px; }
@media (max-width: 1100px) { .metric-rail { grid-template-columns: 1fr; } }
@media (max-width: 700px) { .metric-rail__operations { grid-template-columns: repeat(2, 1fr); }.metric-rail__metric:nth-child(2) { border-right: 0; }.metric-rail__metric:nth-child(-n+2) { border-bottom: 1px solid #e5edf2; } }
</style>
