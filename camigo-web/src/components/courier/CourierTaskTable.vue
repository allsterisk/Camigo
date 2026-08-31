<script setup lang="ts">
import type { CourierDeliveryOrder } from '@/types/delivery-order'
import { getDeliveryStatusDisplay } from '@/utils/delivery-order'
import { formatDateTime } from '@/utils/parcel'

const props = withDefaults(defineProps<{
  tasks: readonly CourierDeliveryOrder[]
  loading?: boolean
  actionOrderId?: number | null
  showActions?: boolean
  timeColumnLabel?: string
}>(), {
  loading: false,
  actionOrderId: null,
  showActions: false,
  timeColumnLabel: '任务时间',
})

defineEmits<{
  pickup: [orderId: number]
  arrival: [orderId: number]
  storage: [orderId: number]
  exception: [orderId: number]
}>()

function executionLabel(status: string): string {
  return ({
    CREATED: '待派单',
    ASSIGNING: '派单中',
    ACCEPTED: '待取件',
    PICKING_UP: '取件中',
    IN_TRANSIT: '配送中',
    ARRIVED: '已到达，待入柜',
    COMPLETED: '已完成',
    FAILED: '配送失败',
    CANCELLED: '已取消',
  } as Record<string, string>)[status] ?? status
}
</script>

<template>
  <el-table :data="tasks" v-loading="loading" row-key="id" stripe>
    <el-table-column label="配送单号" prop="orderNo" min-width="190" />
    <el-table-column label="快递" min-width="205">
      <template #default="{ row }"><div class="courier-task-table__parcel"><strong>{{ row.trackingNo }}</strong><small>{{ row.carrierCode }} · {{ row.sizeClass }} · {{ row.weightKg ?? '—' }} kg</small></div></template>
    </el-table-column>
    <el-table-column label="取件站" min-width="105">
      <template #default="{ row }">{{ row.pickupLocationName }}</template>
    </el-table-column>
    <el-table-column label="配送目的地" min-width="120">
      <template #default="{ row }">{{ row.destinationLocationName }}</template>
    </el-table-column>
    <el-table-column label="任务状态" min-width="155">
      <template #default="{ row }">
        <el-tag :type="getDeliveryStatusDisplay(row.status).type" effect="light" round>
          {{ executionLabel(row.executionStatus) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="props.timeColumnLabel" min-width="170">
      <template #default="{ row }">
        {{ formatDateTime(row.completedTime || row.acceptedTime || row.createTime) }}
      </template>
    </el-table-column>
    <el-table-column v-if="showActions" label="操作" fixed="right" min-width="120">
      <template #default="{ row }">
        <div class="courier-task-table__actions">
        <el-button
          v-if="['ACCEPTED', 'PICKING_UP'].includes(row.executionStatus)"
          type="primary"
          :loading="actionOrderId === row.id"
          :disabled="actionOrderId !== null && actionOrderId !== row.id"
          @click="$emit('pickup', row.id)"
        >确认取件</el-button>
        <el-button
          v-else-if="row.executionStatus === 'IN_TRANSIT'"
          type="success"
          :loading="actionOrderId === row.id"
          :disabled="actionOrderId !== null && actionOrderId !== row.id"
          @click="$emit('arrival', row.id)"
        >确认到达</el-button>
        <el-button
          v-else-if="row.executionStatus === 'ARRIVED'"
          type="primary"
          :loading="actionOrderId === row.id"
          :disabled="actionOrderId !== null && actionOrderId !== row.id"
          @click="$emit('storage', row.id)"
        >选择格口</el-button>
        <el-button
          v-if="['PICKING_UP', 'IN_TRANSIT', 'ARRIVED'].includes(row.executionStatus)"
          type="danger"
          plain
          :disabled="actionOrderId !== null"
          @click="$emit('exception', row.id)"
        >上报异常</el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.courier-task-table__actions { display: flex; flex-wrap: wrap; gap: 8px; }
.courier-task-table__actions :deep(.el-button + .el-button) { margin-left: 0; }
.courier-task-table__parcel { display: grid; gap: 4px; min-width: 0; }
.courier-task-table__parcel strong { color: #284e4a; overflow-wrap: anywhere; }
.courier-task-table__parcel small { color: #758a87; }
</style>
