<script setup lang="ts">
import type { CourierDeliveryOrder } from '@/types/delivery-order'
import {
  formatDeliveryPriority,
  getDeliveryStatusDisplay,
} from '@/utils/delivery-order'
import { formatDateTime } from '@/utils/parcel'

withDefaults(defineProps<{
  tasks: readonly CourierDeliveryOrder[]
  loading?: boolean
  actionOrderId?: number | null
  showActions?: boolean
}>(), {
  loading: false,
  actionOrderId: null,
  showActions: false,
})

defineEmits<{
  accept: [orderId: number]
  complete: [orderId: number]
}>()
</script>

<template>
  <el-table :data="tasks" v-loading="loading" row-key="id" stripe>
    <el-table-column label="配送单号" prop="orderNo" min-width="190" />
    <el-table-column label="快递编号" min-width="105">
      <template #default="{ row }">#{{ row.parcelId }}</template>
    </el-table-column>
    <el-table-column label="目的地" min-width="105">
      <template #default="{ row }">#{{ row.destinationLocationId }}</template>
    </el-table-column>
    <el-table-column label="优先级" min-width="90">
      <template #default="{ row }">{{ formatDeliveryPriority(row.priority) }}</template>
    </el-table-column>
    <el-table-column label="预计耗时" min-width="105">
      <template #default="{ row }">{{ row.predictedMinutes ? `${row.predictedMinutes} 分钟` : '—' }}</template>
    </el-table-column>
    <el-table-column label="状态" min-width="100">
      <template #default="{ row }">
        <el-tag :type="getDeliveryStatusDisplay(row.status).type" effect="light" round>
          {{ getDeliveryStatusDisplay(row.status).label }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="更新时间" min-width="170">
      <template #default="{ row }">
        {{ formatDateTime(row.completedTime || row.acceptedTime || row.createTime) }}
      </template>
    </el-table-column>
    <el-table-column v-if="showActions" label="操作" fixed="right" min-width="120">
      <template #default="{ row }">
        <el-button
          v-if="row.status === 'WAITING'"
          type="primary"
          :loading="actionOrderId === row.id"
          :disabled="actionOrderId !== null && actionOrderId !== row.id"
          @click="$emit('accept', row.id)"
        >接单</el-button>
        <el-button
          v-else-if="row.status === 'PROCESSING'"
          type="success"
          :loading="actionOrderId === row.id"
          :disabled="actionOrderId !== null && actionOrderId !== row.id"
          @click="$emit('complete', row.id)"
        >确认送达</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
