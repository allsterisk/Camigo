<script setup lang="ts">
import type { Parcel } from '@/types/parcel'
import { formatDateTime, getStatusDisplay } from '@/utils/parcel'
defineProps<{ parcels: readonly Parcel[]; loading: boolean; actionId: number | null }>()
defineEmits<{ statusChange: [id: number, status: string] }>()
</script>
<template>
  <el-table :data="parcels" v-loading="loading" row-key="id" stripe>
    <el-table-column label="快递单号" prop="trackingNo" min-width="180" />
    <el-table-column label="承运商" prop="carrierCode" width="100" />
    <el-table-column label="收件用户" min-width="100"><template #default="{ row }">#{{ row.recipientUserId }}</template></el-table-column>
    <el-table-column label="目标柜" min-width="90"><template #default="{ row }">#{{ row.targetLockerId }}</template></el-table-column>
    <el-table-column label="状态" min-width="110"><template #default="{ row }"><el-tag :type="getStatusDisplay(row.currentStatus).type">{{ getStatusDisplay(row.currentStatus).label }}</el-tag></template></el-table-column>
    <el-table-column label="到校时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.arriveTime) }}</template></el-table-column>
    <el-table-column label="操作" fixed="right" min-width="145"><template #default="{ row }"><el-button v-if="row.currentStatus === 'ARRIVED'" type="primary" text :loading="actionId === row.id" @click="$emit('statusChange', row.id, 'DELIVERING')">开始配送</el-button><el-button v-if="row.currentStatus === 'PICKED'" type="success" text :loading="actionId === row.id" @click="$emit('statusChange', row.id, 'COMPLETED')">确认完成</el-button><span v-if="!['ARRIVED','PICKED'].includes(row.currentStatus)">—</span></template></el-table-column>
  </el-table>
</template>
