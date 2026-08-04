<script setup lang="ts">
import { onMounted } from 'vue'

import PageHeader from '@/components/common/PageHeader.vue'
import EvaluationForm from '@/components/user/EvaluationForm.vue'
import { useEvaluation } from '@/composables/useEvaluation'
import { formatDateTime } from '@/utils/parcel'

const {
  loading,
  errorMessage,
  result,
  orders,
  ordersLoading,
  ordersError,
  loadOrders,
  submit,
  reset,
} = useEvaluation()

onMounted(loadOrders)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="SERVICE REVIEW"
      title="配送评价"
      description="为已完成的校园配送留下星级评分和体验反馈。"
    />
    <section v-if="result" class="evaluation-success">
      <span class="evaluation-success__icon">✓</span>
      <h2>评价提交成功</h2>
      <p>配送评分 {{ result.deliveryScore }} 星 · 服务评分 {{ result.serviceScore }} 星</p>
      <small>提交时间：{{ formatDateTime(result.evaluatedTime) }}</small>
      <el-button type="primary" plain @click="reset">继续评价</el-button>
    </section>
    <EvaluationForm
      v-else
      :loading="loading"
      :error-message="errorMessage"
      :orders="orders"
      :orders-loading="ordersLoading"
      :orders-error="ordersError"
      @reload-orders="loadOrders"
      @submit="submit"
    />
  </div>
</template>

<style scoped>
.evaluation-success { min-height: 360px; display: flex; align-items: center; flex-direction: column; justify-content: center; padding: 36px; border: 1px solid #dcebe4; border-radius: 18px; background: #fff; text-align: center; }
.evaluation-success__icon { display: grid; width: 68px; height: 68px; place-items: center; border-radius: 50%; color: #fff; background: #2aaa71; font-size: 30px; box-shadow: 0 14px 30px rgb(42 170 113 / 22%); }
.evaluation-success h2 { margin: 22px 0 8px; color: #20553f; }
.evaluation-success p { margin: 0 0 8px; color: #557565; }
.evaluation-success small { margin-bottom: 24px; color: #93a59c; }
</style>
