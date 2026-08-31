<script setup lang="ts">
import type { ParcelException } from '@/types/parcel-exception'
import { formatDateTime } from '@/utils/parcel'

withDefaults(defineProps<{ records: readonly ParcelException[]; loading?: boolean; actionId?: number | null }>(), {
  loading: false, actionId: null,
})
defineEmits<{ resolve: [record: ParcelException] }>()
const typeLabels: Record<string, string> = {
  PACKAGE_DAMAGED: '包裹破损', RECIPIENT_INFO_ERROR: '收件信息错误', LOCKER_FAILURE: '柜机故障',
  USER_UNREACHABLE: '无法联系用户', OTHER: '其他异常',
}
const statusLabels: Record<string, string> = { PENDING: '待处理', RESOLVED: '已解决', RETURNED: '已退回' }
const resolutionLabels: Record<string, string> = {
  RESUME_DELIVERY: '恢复配送', RETURN_TO_SORTING: '恢复分拣', CONFIRM_RETURNED: '确认退回',
}
</script>

<template>
  <el-table :data="records" v-loading="loading" row-key="id" stripe>
    <el-table-column label="快递 / 配送单" min-width="210">
      <template #default="{ row }"><strong>{{ row.trackingNo }}</strong><br><span class="exception-table__secondary">{{ row.orderNo }}</span></template>
    </el-table-column>
    <el-table-column label="异常" min-width="230">
      <template #default="{ row }"><el-tag type="danger" effect="light">{{ typeLabels[row.exceptionType] || row.exceptionType }}</el-tag><p class="exception-table__description">{{ row.description }}</p></template>
    </el-table-column>
    <el-table-column label="配送员" prop="courierName" min-width="110" />
    <el-table-column label="状态" min-width="105">
      <template #default="{ row }"><el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'RETURNED' ? 'info' : 'success'">{{ statusLabels[row.status] }}</el-tag></template>
    </el-table-column>
    <el-table-column label="记录时间" min-width="175">
      <template #default="{ row }">{{ formatDateTime(row.reportedTime) }}<template v-if="row.handledTime"><br><span class="exception-table__secondary">处理：{{ formatDateTime(row.handledTime) }}</span></template></template>
    </el-table-column>
    <el-table-column label="处理结果" min-width="190">
      <template #default="{ row }"><template v-if="row.resolutionType"><strong>{{ resolutionLabels[row.resolutionType] }}</strong><p class="exception-table__description">{{ row.resolutionNote }}</p></template><span v-else class="exception-table__secondary">等待管理员判断</span></template>
    </el-table-column>
    <el-table-column label="操作" fixed="right" width="112">
      <template #default="{ row }"><el-button v-if="row.status === 'PENDING'" type="primary" :loading="actionId === row.id" :disabled="actionId !== null && actionId !== row.id" @click="$emit('resolve', row)">处理</el-button><span v-else class="exception-table__secondary">已完成</span></template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.exception-table__secondary { color: #71817f; font-size: 13px; }
.exception-table__description { max-width: 34ch; margin: 8px 0 0; color: #526765; line-height: 1.55; overflow-wrap: anywhere; }
</style>
