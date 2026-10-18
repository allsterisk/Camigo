<script setup lang="ts">
import { ArrowDownBold, RefreshRight } from '@element-plus/icons-vue'

import type { CourierRoute } from '@/types/courier-execution'
import { formatDateTime } from '@/utils/parcel'

const props = withDefaults(defineProps<{
  route: Readonly<CourierRoute> | null
  loading: boolean
  errorMessage?: string
  batchActing?: boolean
  stationPickupIds?: readonly number[]
}>(), {
  errorMessage: '',
  batchActing: false,
  stationPickupIds: () => [],
})

const emit = defineEmits<{
  refresh: []
  batchPickup: [orderIds: number[]]
}>()

function formatDuration(seconds: number): string {
  if (seconds <= 0) return '暂无行程'
  const minutes = Math.ceil(seconds / 60)
  return minutes < 60 ? `约 ${minutes} 分钟` : `约 ${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分`
}

function isStationPickupStep(step: { pickupOrderIds?: number[] }): boolean {
  const ids = step.pickupOrderIds ?? []
  return ids.some((id) => props.stationPickupIds.includes(id))
}
</script>

<template>
  <section class="route-panel" aria-labelledby="route-panel-title">
    <header class="route-panel__header">
      <div>
        <h2 id="route-panel-title">当前配送路线</h2>
        <p>人在快递站即可取该站待取件；同站多件可一次取齐，送达仍按路线顺序。</p>
      </div>
      <el-button :icon="RefreshRight" :loading="loading" plain @click="emit('refresh')">
        刷新路线
      </el-button>
    </header>

    <el-alert
      v-if="errorMessage"
      class="route-panel__alert"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <div class="route-panel__body" v-loading="loading">
      <figure class="route-panel__map">
        <img
          src="/images/camigo-campus-route-map.png"
          alt="山东大学软件园校区地点和道路距离示意图"
        />
      </figure>

      <div class="route-panel__itinerary">
        <div class="route-panel__summary">
          <span><strong>{{ route?.totalDistanceMeters ?? 0 }}</strong> 米剩余距离</span>
          <span>{{ formatDuration(route?.estimatedDurationSeconds ?? 0) }}</span>
          <span v-if="route?.versionNo">路线 v{{ route.versionNo }}</span>
        </div>

        <div class="route-panel__current">
          <span class="route-panel__dot route-panel__dot--current" />
          <div>
            <small>当前位置</small>
            <strong>{{ route?.currentLocationName || '尚未确认' }}</strong>
          </div>
        </div>

        <div v-if="route?.steps.length" class="route-panel__steps">
          <div v-for="step in route.steps" :key="`${step.sequenceNo}-${step.locationId}`">
            <el-icon class="route-panel__arrow" aria-hidden="true"><ArrowDownBold /></el-icon>
            <article class="route-panel__step">
              <span class="route-panel__dot" />
              <div class="route-panel__step-copy">
                <div class="route-panel__step-title">
                  <strong>{{ step.locationName }}</strong>
                  <time v-if="step.estimatedArrivalTime">
                    预计 {{ formatDateTime(step.estimatedArrivalTime) }}
                  </time>
                </div>
                <p v-if="step.pickupOrderNos.length">取件：{{ step.pickupOrderNos.join('、') }}</p>
                <p v-if="step.dropoffOrderNos.length">送达：{{ step.dropoffOrderNos.join('、') }}</p>
                <el-button
                  v-if="isStationPickupStep(step) && stationPickupIds.length > 1"
                  class="route-panel__batch"
                  type="primary"
                  size="small"
                  :loading="batchActing"
                  :disabled="batchActing"
                  @click="emit('batchPickup', [...stationPickupIds])"
                >
                  本站全部取件（{{ stationPickupIds.length }}）
                </el-button>
              </div>
            </article>
          </div>
        </div>

        <div v-else class="route-panel__empty">
          <strong>当前没有待行进节点</strong>
          <p>接受新派单后，系统会在这里显示新的取送顺序。</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.route-panel { padding: 24px; border-radius: 16px; background: #fff; box-shadow: 0 14px 38px rgb(16 75 69 / 8%); }
.route-panel__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 20px; }
.route-panel__header h2 { margin: 0; color: #1f4e49; font-size: 20px; letter-spacing: -.015em; }
.route-panel__header p { max-width: 64ch; margin: 7px 0 0; color: #6d8884; font-size: 13px; line-height: 1.6; }
.route-panel__alert { margin-bottom: 16px; }
.route-panel__body { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(330px, .85fr); gap: 26px; min-height: 330px; }
.route-panel__map { overflow: hidden; margin: 0; border-radius: 14px; background: #deeee7; }
.route-panel__map img { display: block; width: 100%; height: 100%; object-fit: contain; }
.route-panel__itinerary { min-width: 0; padding: 4px 2px; }
.route-panel__summary { display: flex; flex-wrap: wrap; gap: 8px 18px; padding-bottom: 16px; border-bottom: 1px solid #e5efec; color: #66827e; font-size: 12px; }
.route-panel__summary strong { color: #087f73; font-size: 18px; }
.route-panel__current, .route-panel__step { display: flex; align-items: flex-start; gap: 13px; }
.route-panel__current { padding: 20px 0 4px; }
.route-panel__current small, .route-panel__current strong { display: block; }
.route-panel__current small { margin-bottom: 3px; color: #7e9692; font-size: 11px; }
.route-panel__current strong { color: #204c47; font-size: 15px; }
.route-panel__dot { flex: 0 0 11px; width: 11px; height: 11px; margin-top: 5px; border: 3px solid #d9f0eb; border-radius: 50%; background: #0a9182; box-sizing: content-box; }
.route-panel__dot--current { border-color: #ffe4da; background: #ec6540; }
.route-panel__arrow { display: block; margin: 7px 0 7px 1px; color: #8eb3ad; font-size: 14px; }
.route-panel__step { padding: 13px 14px; border-radius: 13px; background: #f4f9f7; }
.route-panel__step-copy { min-width: 0; flex: 1; }
.route-panel__step-title { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.route-panel__step-title strong { color: #244f4b; font-size: 14px; }
.route-panel__step-title time { flex: 0 0 auto; color: #78918d; font-size: 10px; }
.route-panel__step p { margin: 7px 0 0; color: #577672; font-size: 12px; line-height: 1.5; }
.route-panel__batch { margin-top: 12px; }
.route-panel__empty { padding: 42px 20px; color: #77908c; text-align: center; }
.route-panel__empty strong { display: block; color: #456a65; font-size: 14px; }
.route-panel__empty p { max-width: 34ch; margin: 8px auto 0; font-size: 12px; line-height: 1.6; }

@media (max-width: 980px) {
  .route-panel__body { grid-template-columns: 1fr; }
  .route-panel__map { min-height: 280px; }
}

@media (max-width: 560px) {
  .route-panel { padding: 18px; }
  .route-panel__header { align-items: stretch; flex-direction: column; gap: 14px; }
  .route-panel__map { min-height: 210px; }
  .route-panel__step-title { align-items: flex-start; flex-direction: column; gap: 4px; }
}
</style>
