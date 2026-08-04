<script setup lang="ts">
import { reactive, useTemplateRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import type { EvaluationPayload } from '@/types/evaluation'

defineProps<{ loading: boolean; errorMessage: string }>()
const emit = defineEmits<{ submit: [payload: EvaluationPayload] }>()

const formRef = useTemplateRef<FormInstance>('formRef')
const form = reactive<EvaluationPayload>({
  deliveryOrderId: 0,
  deliveryScore: 5,
  serviceScore: 5,
  content: '',
})

const rules: FormRules<EvaluationPayload> = {
  deliveryOrderId: [
    { required: true, message: '请输入配送订单编号', trigger: 'blur' },
    { type: 'number', min: 1, message: '配送订单编号必须大于 0', trigger: 'blur' },
  ],
  deliveryScore: [{ required: true, message: '请选择配送评分', trigger: 'change' }],
  serviceScore: [{ required: true, message: '请选择服务评分', trigger: 'change' }],
  content: [{ max: 500, message: '评价内容不能超过 500 位', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid) emit('submit', { ...form, content: form.content.trim() })
}
</script>

<template>
  <section class="evaluation-form-card">
    <el-alert
      title="当前后端尚未提供学生待评价订单列表，请填写已完成配送的订单编号。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-alert v-if="errorMessage" class="evaluation-form-card__error" :title="errorMessage" type="error" :closable="false" show-icon />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="handleSubmit">
      <el-form-item label="配送订单编号" prop="deliveryOrderId">
        <el-input-number v-model="form.deliveryOrderId" :min="0" :max="999999999" controls-position="right" />
      </el-form-item>
      <div class="evaluation-form-card__scores">
        <el-form-item label="配送效率" prop="deliveryScore">
          <el-rate v-model="form.deliveryScore" show-text :texts="['很慢', '较慢', '一般', '很快', '非常快']" />
        </el-form-item>
        <el-form-item label="服务体验" prop="serviceScore">
          <el-rate v-model="form.serviceScore" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
        </el-form-item>
      </div>
      <el-form-item label="评价内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="分享本次校园配送体验（选填）" />
      </el-form-item>
      <el-button class="evaluation-form-card__submit" type="primary" native-type="submit" :loading="loading">提交评价</el-button>
    </el-form>
  </section>
</template>

<style scoped>
.evaluation-form-card { padding: 26px; border: 1px solid #dfebf2; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgb(15 59 92 / 5%); }
.evaluation-form-card > :deep(.el-alert) { margin-bottom: 24px; }
.evaluation-form-card__error { margin-top: -12px; }
.evaluation-form-card__scores { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.evaluation-form-card__submit { min-width: 150px; }

@media (max-width: 620px) {
  .evaluation-form-card { padding: 20px; }
  .evaluation-form-card__scores { grid-template-columns: 1fr; gap: 0; }
}
</style>
