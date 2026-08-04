<script setup lang="ts">
import type { AdminCourier } from '@/types/admin'
defineProps<{ couriers: readonly AdminCourier[]; loading: boolean }>()
function formatDuration(seconds: number | null) { return seconds == null ? '—' : `${Math.round(seconds / 60)} 分钟` }
</script>
<template>
  <el-table :data="couriers" v-loading="loading" row-key="id" stripe>
    <el-table-column label="配送员" min-width="150"><template #default="{ row }"><strong>{{ row.realName }}</strong><br><small>{{ row.courierNo }}</small></template></el-table-column>
    <el-table-column label="手机" prop="phone" min-width="130" />
    <el-table-column label="在职状态" prop="employmentStatus" min-width="100" />
    <el-table-column label="接单状态" prop="availabilityStatus" min-width="110" />
    <el-table-column label="订单总数" prop="totalOrders" min-width="95" />
    <el-table-column label="完成数" prop="completedOrders" min-width="85" />
    <el-table-column label="平均耗时" min-width="105"><template #default="{ row }">{{ formatDuration(row.avgDeliverySeconds) }}</template></el-table-column>
    <el-table-column label="评分" min-width="90"><template #default="{ row }">{{ row.ratingAvg ?? '—' }}</template></el-table-column>
  </el-table>
</template>
<style scoped>small { color: #8695a2; }</style>
