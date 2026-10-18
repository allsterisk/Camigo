<script setup lang="ts">
import { computed } from 'vue'

import StatusTag from '@/components/common/StatusTag.vue'
import type { Parcel, ParcelStatus } from '@/types/parcel'
import { formatCarrier, formatDateTime, formatSize, getStatusDisplay } from '@/utils/parcel'

const props = defineProps<{ parcel: Parcel }>()

const statusSteps: Array<{ status: ParcelStatus; title: string }> = [
  { status: 'ARRIVED', title: '到达校园' },
  { status: 'DELIVERING', title: '校园配送' },
  { status: 'STORED', title: '存入快递柜' },
  { status: 'PICKED', title: '完成取件' },
  { status: 'COMPLETED', title: '服务完成' },
]

const activeStep = computed(() => {
  const index = statusSteps.findIndex((item) => item.status === props.parcel.currentStatus)
  return index < 0 ? 0 : index + 1
})
const statusDisplay = computed(() => getStatusDisplay(props.parcel.currentStatus))
</script>

<template>
  <div class="parcel-detail">
    <section class="parcel-detail__hero">
      <div>
        <span class="parcel-detail__label">快递单号</span>
        <strong class="parcel-detail__tracking">{{ parcel.trackingNo }}</strong>
        <p>{{ statusDisplay.description }}</p>
      </div>
      <StatusTag :status="parcel.currentStatus" />
    </section>

    <section class="parcel-detail__section">
      <h2>配送进度</h2>
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="item in statusSteps" :key="item.status" :title="item.title" />
      </el-steps>
      <p class="parcel-detail__timeline-note">当前接口仅提供最新状态；以上为标准流程进度，并非完整物流事件历史。</p>
    </section>

    <section class="parcel-detail__section">
      <h2>快递信息</h2>
      <dl class="parcel-detail__grid">
        <div><dt>承运商</dt><dd>{{ formatCarrier(parcel.carrierCode) }}</dd></div>
        <div><dt>寄件人</dt><dd>{{ parcel.senderName }}</dd></div>
        <div><dt>联系方式</dt><dd>{{ parcel.senderPhoneMasked || '已保护' }}</dd></div>
        <div><dt>包裹规格</dt><dd>{{ formatSize(parcel.sizeClass) }}</dd></div>
        <div><dt>包裹重量</dt><dd>{{ parcel.weightKg === null ? '未登记' : `${parcel.weightKg} kg` }}</dd></div>
        <div><dt>到校时间</dt><dd>{{ formatDateTime(parcel.arriveTime) }}</dd></div>
      </dl>
    </section>

    <section class="parcel-detail__section parcel-detail__pickup">
      <div>
        <span class="parcel-detail__label">取件位置</span>
        <h2>{{ parcel.targetLockerAddress || '详细地址待确认' }}</h2>
        <p>{{ parcel.targetLockerName || '智能柜待确认' }} · 请在收到取件通知后前往</p>
      </div>
      <span class="parcel-detail__locker">柜</span>
    </section>
  </div>
</template>

<style scoped>
.parcel-detail { display: grid; gap: 20px; }
.parcel-detail__hero, .parcel-detail__section { padding: 26px; border: 1px solid #dfebf2; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgb(15 59 92 / 5%); }
.parcel-detail__hero { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.parcel-detail__label { color: #8295a5; font-size: 11px; font-weight: 700; letter-spacing: .12em; }
.parcel-detail__tracking { display: block; margin: 8px 0; color: #123f63; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: clamp(22px, 4vw, 30px); }
.parcel-detail__hero p, .parcel-detail__pickup p { margin: 0; color: #74899b; line-height: 1.7; }
.parcel-detail__section h2 { margin: 0 0 24px; color: #224b6c; font-size: 18px; }
.parcel-detail__timeline-note { margin: 20px 0 0; color: #96a5b1; font-size: 11px; text-align: center; }
.parcel-detail__grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 22px; margin: 0; }
.parcel-detail__grid div { min-width: 0; }
.parcel-detail__grid dt { margin-bottom: 7px; color: #899aa8; font-size: 11px; }
.parcel-detail__grid dd { margin: 0; overflow-wrap: anywhere; color: #294e6c; font-size: 14px; font-weight: 600; }
.parcel-detail__pickup { display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, #f3faff, #fff); }
.parcel-detail__pickup h2 { margin: 7px 0; font-size: 22px; }
.parcel-detail__locker { display: grid; flex: 0 0 62px; height: 62px; place-items: center; border-radius: 18px; color: #fff; background: #0b6d9f; font-size: 20px; font-weight: 800; box-shadow: 0 12px 24px rgb(11 109 159 / 20%); }

@media (max-width: 700px) {
  .parcel-detail__grid { grid-template-columns: 1fr 1fr; }
  .parcel-detail__section { padding: 20px; }
  .parcel-detail__section :deep(.el-step__title) { font-size: 10px; }
}

@media (max-width: 460px) {
  .parcel-detail__grid { grid-template-columns: 1fr; }
}
</style>
