<script setup lang="ts">
import type { DeliveryOrderStatus } from '@/types/delivery-order'

const status = defineModel<Extract<DeliveryOrderStatus, 'WAITING' | 'PROCESSING'>>('status', {
  required: true,
})

defineProps<{ loading: boolean }>()
</script>

<template>
  <section class="courier-task-filters">
    <el-radio-group v-model="status" :disabled="loading" size="large">
      <el-radio-button value="WAITING">可接任务</el-radio-button>
      <el-radio-button value="PROCESSING">进行中</el-radio-button>
    </el-radio-group>
    <span>切换任务状态后，筛选条件会保留在当前网址中。</span>
  </section>
</template>

<style scoped>
.courier-task-filters { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; padding: 16px 20px; border: 1px solid #dfeae9; border-radius: 16px; background: #fff; }
.courier-task-filters span { color: #879997; font-size: 12px; }

@media (max-width: 640px) {
  .courier-task-filters { align-items: flex-start; flex-direction: column; }
}
</style>
