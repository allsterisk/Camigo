<script setup lang="ts">
import type { UserDeliveryOrder } from '@/types/delivery-order'

defineProps<{
  modelValue: number
  orders: readonly UserDeliveryOrder[]
  loading: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number]
  retry: []
}>()

function orderLabel(order: UserDeliveryOrder) {
  const courier = order.courierName ? ` · 配送员 ${order.courierName}` : ''
  return `${order.trackingNo} · ${order.orderNo}${courier}`
}
</script>

<template>
  <div class="evaluation-order-picker">
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button link type="primary" @click="emit('retry')">重新加载</el-button>
      </template>
    </el-alert>

    <el-select
      :model-value="modelValue || undefined"
      :loading="loading"
      :disabled="loading || Boolean(errorMessage)"
      filterable
      placeholder="请选择已完成且待评价的配送订单"
      no-data-text="暂无待评价配送订单"
      @update:model-value="emit('update:modelValue', $event)"
    >
      <el-option
        v-for="order in orders"
        :key="order.deliveryOrderId"
        :label="orderLabel(order)"
        :value="order.deliveryOrderId"
      />
    </el-select>
    <p v-if="!loading && !errorMessage && orders.length === 0">
      暂无待评价订单。配送完成后，订单会自动出现在这里。
    </p>
  </div>
</template>

<style scoped>
.evaluation-order-picker :deep(.el-select) { width: 100%; }
.evaluation-order-picker > :deep(.el-alert) { margin-bottom: 12px; }
.evaluation-order-picker p { margin: 10px 0 0; color: #80939f; font-size: 13px; }
</style>
