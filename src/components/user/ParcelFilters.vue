<script setup lang="ts">
import type { ParcelStatus } from '@/types/parcel'

const trackingNo = defineModel<string>('trackingNo', { required: true })
const status = defineModel<ParcelStatus | ''>('status', { required: true })

defineProps<{ loading: boolean }>()
defineEmits<{ search: []; reset: [] }>()

const statusOptions: Array<{ label: string; value: ParcelStatus }> = [
  { label: '已到校', value: 'ARRIVED' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '待取件', value: 'STORED' },
  { label: '已取件', value: 'PICKED' },
  { label: '已完成', value: 'COMPLETED' },
]
</script>

<template>
  <section class="parcel-filters">
    <el-input v-model.trim="trackingNo" clearable placeholder="搜索快递单号" @keyup.enter="$emit('search')" />
    <el-select v-model="status" placeholder="全部状态" clearable>
      <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-button type="primary" :loading="loading" @click="$emit('search')">查询</el-button>
    <el-button @click="$emit('reset')">重置</el-button>
  </section>
</template>

<style scoped>
.parcel-filters { display: grid; grid-template-columns: minmax(220px, 1fr) 170px auto auto; gap: 12px; margin-bottom: 20px; padding: 18px; border: 1px solid #e1ebf2; border-radius: 16px; background: #fff; }

@media (max-width: 720px) {
  .parcel-filters { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 480px) {
  .parcel-filters { grid-template-columns: 1fr; }
}
</style>
