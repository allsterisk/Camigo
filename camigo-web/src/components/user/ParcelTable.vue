<script setup lang="ts">
import StatusTag from '@/components/common/StatusTag.vue'
import type { Parcel } from '@/types/parcel'
import { formatCarrier, formatDateTime } from '@/utils/parcel'

defineProps<{
  parcels: readonly Parcel[]
  loading: boolean
}>()

defineEmits<{ view: [id: number] }>()
</script>

<template>
  <el-table v-loading="loading" :data="parcels" class="parcel-table" row-key="id">
    <el-table-column prop="trackingNo" label="快递单号" min-width="180">
      <template #default="scope"><strong class="parcel-table__tracking">{{ scope.row.trackingNo }}</strong></template>
    </el-table-column>
    <el-table-column label="承运商" min-width="120">
      <template #default="scope">{{ formatCarrier(scope.row.carrierCode) }}</template>
    </el-table-column>
    <el-table-column label="状态" min-width="110">
      <template #default="scope"><StatusTag :status="scope.row.currentStatus" /></template>
    </el-table-column>
    <el-table-column label="到校时间" min-width="170">
      <template #default="scope">{{ formatDateTime(scope.row.arriveTime) }}</template>
    </el-table-column>
    <el-table-column label="取件方式" min-width="120">
      <template #default>快递柜取件</template>
    </el-table-column>
    <el-table-column label="取件地点" min-width="150">
      <template #default="scope">{{ scope.row.targetLockerName || '—' }}</template>
    </el-table-column>
    <el-table-column label="操作" width="90" fixed="right">
      <template #default="scope"><el-button link type="primary" @click="$emit('view', scope.row.id)">详情</el-button></template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.parcel-table { width: 100%; border-radius: 14px; }
.parcel-table__tracking { color: #244c6c; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
</style>
