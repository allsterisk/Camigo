<script setup lang="ts">
import type { AdminLocker } from '@/types/admin'
import { formatDateTime } from '@/utils/parcel'
defineProps<{ lockers: readonly AdminLocker[]; loading: boolean }>()
</script>
<template>
  <el-table :data="lockers" v-loading="loading" row-key="id" stripe>
    <el-table-column label="快递柜" min-width="160"><template #default="{ row }"><strong>{{ row.lockerName }}</strong><br><small>{{ row.lockerCode }}</small></template></el-table-column>
    <el-table-column label="位置" min-width="140"><template #default="{ row }">{{ row.locationName || `位置 #${row.locationId}` }}</template></el-table-column>
    <el-table-column label="运行状态" min-width="105"><template #default="{ row }"><el-tag :type="row.operationStatus === 'ONLINE' ? 'success' : 'warning'">{{ row.operationStatus }}</el-tag></template></el-table-column>
    <el-table-column label="格口" min-width="180"><template #default="{ row }">空闲 {{ row.availableSlots }} / 占用 {{ row.occupiedSlots }} / 异常 {{ row.unavailableSlots }}</template></el-table-column>
    <el-table-column label="使用率" min-width="180"><template #default="{ row }"><el-progress :percentage="Number(row.usageRate)" :stroke-width="10" /></template></el-table-column>
    <el-table-column label="最后在线时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.lastHeartbeatTime) }}</template></el-table-column>
  </el-table>
</template>
<style scoped>small { color: #8695a2; }</style>
