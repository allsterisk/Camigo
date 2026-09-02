<script setup lang="ts">
import type { Parcel } from '@/types/parcel'
import { formatDateTime, getStatusDisplay } from '@/utils/parcel'
defineProps<{ parcels: readonly Parcel[]; loading: boolean; actionId: number | null }>()
defineEmits<{
  statusChange: [id: number, status: string]
  startDelivery: [parcelId: number]
  dispatchDetails: [parcelId: number]
}>()
</script>
<template>
  <el-table :data="parcels" v-loading="loading" row-key="id" stripe>
    <el-table-column label="快递单号" prop="trackingNo" min-width="180" />
    <el-table-column label="承运商" prop="carrierCode" width="100" />
    <el-table-column label="姓名" min-width="100"><template #default="{ row }">{{ row.recipientName || '—' }}</template></el-table-column>
    <el-table-column label="智能柜" min-width="140"><template #default="{ row }">{{ row.targetLockerName || '—' }}</template></el-table-column>
    <el-table-column label="状态" min-width="110"><template #default="{ row }"><el-tag :type="getStatusDisplay(row.currentStatus).type">{{ getStatusDisplay(row.currentStatus).label }}</el-tag></template></el-table-column>
    <el-table-column label="到校时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.arriveTime) }}</template></el-table-column>
    <el-table-column label="操作" fixed="right" min-width="180"><template #default="{ row }"><el-button type="primary" link @click="$emit('dispatchDetails', row.id)">派单详情</el-button><el-button v-if="row.currentStatus === 'ARRIVED' && row.deliveryStartAvailable" type="primary" text :loading="actionId === row.id" @click="$emit('startDelivery', row.id)">开始配送</el-button><el-button v-if="row.currentStatus === 'PICKED'" type="success" text :loading="actionId === row.id" @click="$emit('statusChange', row.id, 'COMPLETED')">确认完成</el-button></template></el-table-column>
  </el-table>
</template>
