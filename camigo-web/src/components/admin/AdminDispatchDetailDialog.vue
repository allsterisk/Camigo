<script setup lang="ts">
import type { DispatchAssignmentDetail } from '@/types/dispatch'
import { formatDateTime } from '@/utils/parcel'

const visible = defineModel<boolean>({ required: true })
const props = withDefaults(defineProps<{
  parcelId?: number | null
  records: readonly DispatchAssignmentDetail[]
  loading?: boolean
  errorMessage?: string
}>(), { parcelId: null, loading: false, errorMessage: '' })
const emit = defineEmits<{ closed: [] }>()

const decisionLabels: Record<string, string> = {
  CANDIDATE: '候选', OFFERED: '已邀请', ACCEPTED: '已接受',
  REJECTED: '已拒绝', EXPIRED: '已超时', SKIPPED: '未入选',
}
function score(value: number | null | undefined) {
  return value === null || value === undefined ? '—' : Number(value).toFixed(2)
}
</script>

<template>
  <el-dialog v-model="visible" title="智能派单详情" width="min(1120px, calc(100vw - 32px))" :close-on-click-modal="!loading" @closed="emit('closed')">
    <p class="dispatch-detail__intro">包裹 #{{ props.parcelId }} 的最新配送单候选快照。所有分数、轮次和决策均来自派单时保存的审计记录。</p>
    <el-alert v-if="errorMessage" class="dispatch-detail__alert" :title="errorMessage" type="error" :closable="false" show-icon />
    <el-table :data="records" v-loading="loading" row-key="assignmentId" stripe max-height="560" empty-text="该配送单尚未形成候选记录">
      <el-table-column label="轮次" prop="assignmentRound" width="70" />
      <el-table-column label="配送员" min-width="150"><template #default="{ row }"><div class="dispatch-detail__courier"><strong>{{ row.courierName }}</strong><small>{{ row.courierNo }}</small></div></template></el-table-column>
      <el-table-column label="决策" min-width="95"><template #default="{ row }"><el-tag :type="row.decision === 'ACCEPTED' ? 'success' : row.decision === 'OFFERED' ? 'warning' : row.decision === 'REJECTED' ? 'danger' : 'info'" effect="plain">{{ decisionLabels[row.decision] || row.decision }}</el-tag></template></el-table-column>
      <el-table-column label="路线新增" min-width="105"><template #default="{ row }">{{ row.routeIncrementMeters === null ? '历史未记录' : `${row.routeIncrementMeters} 米` }}</template></el-table-column>
      <el-table-column label="活动订单" min-width="90"><template #default="{ row }">{{ row.activeOrderCount ?? '历史未记录' }}</template></el-table-column>
      <el-table-column label="距离分" min-width="78"><template #default="{ row }">{{ score(row.distanceScore) }}</template></el-table-column>
      <el-table-column label="负载分" min-width="78"><template #default="{ row }">{{ score(row.workloadScore) }}</template></el-table-column>
      <el-table-column label="服务分" min-width="78"><template #default="{ row }">{{ score(row.serviceScore) }}</template></el-table-column>
      <el-table-column label="综合分" min-width="82"><template #default="{ row }"><strong>{{ score(row.totalScore) }}</strong></template></el-table-column>
      <el-table-column label="算法版本" prop="algorithmVersion" min-width="150" />
      <el-table-column label="邀请/响应" min-width="205"><template #default="{ row }"><div class="dispatch-detail__times"><span>邀请 {{ row.offerTime ? formatDateTime(row.offerTime) : '—' }}</span><span>截止 {{ row.offerExpireTime ? formatDateTime(row.offerExpireTime) : '—' }}</span><span>响应 {{ row.responseTime ? formatDateTime(row.responseTime) : '—' }}</span></div></template></el-table-column>
      <el-table-column label="选择依据 / 拒绝原因" min-width="220"><template #default="{ row }"><div class="dispatch-detail__reason"><span>{{ row.decisionReason || '—' }}</span><small v-if="row.rejectReasonCode">{{ row.rejectReasonCode }}{{ row.rejectReasonDetail ? `：${row.rejectReasonDetail}` : '' }}</small></div></template></el-table-column>
    </el-table>
    <template #footer><el-button @click="visible = false">关闭</el-button></template>
  </el-dialog>
</template>

<style scoped>
.dispatch-detail__intro { max-width: 72ch; margin: -4px 0 18px; color: #627a77; line-height: 1.65; }
.dispatch-detail__alert { margin-bottom: 16px; }
.dispatch-detail__courier, .dispatch-detail__times, .dispatch-detail__reason { display: grid; gap: 4px; min-width: 0; }
.dispatch-detail__courier strong { color: #2b514d; }
.dispatch-detail__courier small, .dispatch-detail__times, .dispatch-detail__reason small { color: #788d8a; }
.dispatch-detail__times { font-size: 12px; }
.dispatch-detail__reason span, .dispatch-detail__reason small { overflow-wrap: anywhere; }
</style>
