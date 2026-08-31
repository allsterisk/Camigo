<script setup lang="ts">
import { reactive, watch } from 'vue'
import { Timer } from '@element-plus/icons-vue'
import type { DispatchOffer, DispatchRejectReason } from '@/types/dispatch'

interface RejectionState {
  reasonCode: DispatchRejectReason
  reasonDetail: string
}

const props = defineProps<{
  offers: readonly DispatchOffer[]
  remainingSecondsById: Readonly<Record<number, number>>
  loading?: boolean
  submittingAssignmentId?: number | null
  errorMessage?: string
}>()
const emit = defineEmits<{
  refresh: []
  accept: [assignmentId: number]
  reject: [assignmentId: number, reasonCode: DispatchRejectReason, reasonDetail?: string]
}>()
const rejections = reactive<Record<number, RejectionState>>({})

watch(
  () => props.offers,
  (offers) => {
    const activeIds = new Set(offers.map((offer) => offer.assignmentId))
    for (const offer of offers) {
      rejections[offer.assignmentId] ??= { reasonCode: 'TOO_BUSY', reasonDetail: '' }
    }
    for (const assignmentId of Object.keys(rejections).map(Number)) {
      if (!activeIds.has(assignmentId)) delete rejections[assignmentId]
    }
  },
  { immediate: true },
)

function remainingSeconds(assignmentId: number): number {
  return props.remainingSecondsById[assignmentId] ?? 0
}

function countdown(assignmentId: number): string {
  const secondsLeft = remainingSeconds(assignmentId)
  const minutes = Math.floor(secondsLeft / 60)
  const seconds = secondsLeft % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function canReject(assignmentId: number): boolean {
  const rejection = rejections[assignmentId]
  return Boolean(rejection
    && (rejection.reasonCode !== 'OTHER' || rejection.reasonDetail.trim().length > 0))
}

function reject(assignmentId: number) {
  const rejection = rejections[assignmentId]
  if (!rejection) return
  emit('reject', assignmentId, rejection.reasonCode, rejection.reasonDetail.trim() || undefined)
}
</script>

<template>
  <section class="dispatch-offers" aria-labelledby="dispatch-offers-title" aria-live="polite">
    <div class="dispatch-offers__topline">
      <div>
        <h2 id="dispatch-offers-title">
          待确认派单<span v-if="offers.length">（{{ offers.length }}）</span>
        </h2>
        <p>系统分配给你的全部有效邀请均显示在这里，每单独立倒计时和确认。</p>
      </div>
      <el-button link type="primary" :loading="loading" @click="emit('refresh')">刷新邀请</el-button>
    </div>
    <el-alert v-if="errorMessage" class="dispatch-offers__alert" :title="errorMessage"
      type="error" :closable="false" show-icon />
    <div v-if="offers.length" class="dispatch-offers__list">
      <article v-for="offer in offers" :key="offer.assignmentId" class="dispatch-offer">
        <div class="dispatch-offer__countdown"
          :class="{ 'dispatch-offer__countdown--urgent': remainingSeconds(offer.assignmentId) <= 30 }">
          <el-icon aria-hidden="true"><Timer /></el-icon>
          <div><small>确认剩余时间</small><strong>{{ countdown(offer.assignmentId) }}</strong></div>
        </div>
        <div class="dispatch-offer__route">
          <div><small>取件服务站</small><strong>{{ offer.pickupLocationName }}</strong></div>
          <span aria-hidden="true">→</span>
          <div><small>配送目的地</small><strong>{{ offer.destinationLocationName }}</strong></div>
        </div>
        <dl class="dispatch-offer__facts">
          <div><dt>配送单号</dt><dd>{{ offer.orderNo }}</dd></div>
          <div><dt>路线新增</dt><dd>{{ offer.routeIncrementMeters }} 米</dd></div>
          <div><dt>综合得分</dt><dd>{{ Number(offer.totalScore).toFixed(2) }}</dd></div>
        </dl>
        <div class="dispatch-offer__decision">
          <el-button type="primary" size="large"
            :loading="submittingAssignmentId === offer.assignmentId"
            :disabled="remainingSeconds(offer.assignmentId) === 0 || submittingAssignmentId !== null"
            @click="emit('accept', offer.assignmentId)">接受派单</el-button>
          <el-select v-model="rejections[offer.assignmentId].reasonCode" aria-label="拒绝原因"
            :disabled="submittingAssignmentId !== null">
            <el-option label="当前任务较多" value="TOO_BUSY" />
            <el-option label="距离过远" value="TOO_FAR" />
            <el-option label="车辆或设备问题" value="VEHICLE_ISSUE" />
            <el-option label="其他原因" value="OTHER" />
          </el-select>
          <el-input v-if="rejections[offer.assignmentId].reasonCode === 'OTHER'"
            v-model="rejections[offer.assignmentId].reasonDetail" maxlength="200"
            placeholder="请填写具体原因" aria-label="其他拒绝原因" />
          <el-button plain
            :loading="submittingAssignmentId === offer.assignmentId"
            :disabled="remainingSeconds(offer.assignmentId) === 0 || submittingAssignmentId !== null || !canReject(offer.assignmentId)"
            @click="reject(offer.assignmentId)">拒绝本次邀请</el-button>
        </div>
      </article>
    </div>
    <div v-else class="dispatch-offers__empty" v-loading="loading">
      <strong>当前没有待确认邀请</strong>
      <span>新邀请到达后会在这里显示3分钟倒计时。</span>
    </div>
  </section>
</template>

<style scoped>
.dispatch-offers { margin-bottom: 22px; padding: 24px; border: 1px solid #d7e8e5; border-radius: 16px; background: #fff; box-shadow: 0 12px 32px rgb(12 71 68 / 6%); }
.dispatch-offers__topline { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.dispatch-offers__topline h2 { margin: 0; color: #204d49; font-size: 20px; }
.dispatch-offers__topline p { max-width: 64ch; margin: 6px 0 0; color: #6d8581; font-size: 13px; line-height: 1.6; }
.dispatch-offers__alert { margin-bottom: 16px; }
.dispatch-offers__list { display: grid; gap: 14px; }
.dispatch-offer { display: grid; grid-template-columns: 170px minmax(260px, 1fr); gap: 18px 24px; padding: 20px; border: 1px solid #e1ecea; border-radius: 14px; background: #fbfdfd; }
.dispatch-offer__countdown { display: flex; align-items: center; gap: 12px; grid-row: span 2; padding: 18px; border-radius: 14px; background: #edf8f5; color: #08776d; }
.dispatch-offer__countdown--urgent { background: #fff2ec; color: #c44d2e; }
.dispatch-offer__countdown .el-icon { font-size: 24px; }
.dispatch-offer__countdown small, .dispatch-offer__countdown strong { display: block; }
.dispatch-offer__countdown small { margin-bottom: 5px; font-size: 12px; }
.dispatch-offer__countdown strong { font-variant-numeric: tabular-nums; font-size: 31px; letter-spacing: -.02em; }
.dispatch-offer__route { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 14px; }
.dispatch-offer__route div { min-width: 0; }
.dispatch-offer__route small, .dispatch-offer__route strong { display: block; }
.dispatch-offer__route small { margin-bottom: 4px; color: #7d9290; font-size: 11px; }
.dispatch-offer__route strong { color: #244d49; overflow-wrap: anywhere; }
.dispatch-offer__route span { color: #6fa39c; }
.dispatch-offer__facts { display: flex; flex-wrap: wrap; gap: 10px 28px; margin: 0; }
.dispatch-offer__facts div { display: flex; gap: 7px; }
.dispatch-offer__facts dt { color: #7b918d; }
.dispatch-offer__facts dd { margin: 0; color: #3d625e; font-weight: 700; overflow-wrap: anywhere; }
.dispatch-offer__decision { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; grid-column: 1 / -1; padding-top: 4px; }
.dispatch-offer__decision .el-select { width: 170px; }
.dispatch-offer__decision .el-input { width: min(280px, 100%); }
.dispatch-offers__empty { display: grid; place-content: center; min-height: 110px; color: #79908c; text-align: center; }
.dispatch-offers__empty strong { color: #456b66; }
.dispatch-offers__empty span { margin-top: 6px; font-size: 13px; }
@media (max-width: 720px) {
  .dispatch-offers { padding: 18px; }
  .dispatch-offer { grid-template-columns: 1fr; padding: 16px; }
  .dispatch-offer__countdown { grid-row: auto; }
  .dispatch-offer__decision { grid-column: auto; align-items: stretch; flex-direction: column; }
  .dispatch-offer__decision .el-select, .dispatch-offer__decision .el-input, .dispatch-offer__decision .el-button { width: 100%; }
}
</style>
