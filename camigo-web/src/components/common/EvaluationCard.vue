<script setup lang="ts">
import { computed } from 'vue'

import type { Evaluation } from '@/types/evaluation'
import { formatDateTime } from '@/utils/parcel'

const props = withDefaults(defineProps<{
  evaluation: Evaluation | null
  emptyText?: string
  bordered?: boolean
}>(), {
  emptyText: '暂无评价',
  bordered: true,
})

const evaluatedTimeText = computed(() =>
  props.evaluation ? formatDateTime(props.evaluation.evaluatedTime) : '',
)
</script>

<template>
  <div v-if="evaluation" class="evaluation-card" :class="{ 'evaluation-card--plain': !bordered }">
    <div class="evaluation-card__scores">
      <div class="evaluation-card__score">
        <span class="evaluation-card__label">配送效率</span>
        <el-rate :model-value="evaluation.deliveryScore" disabled :size="16" />
        <span class="evaluation-card__value">{{ evaluation.deliveryScore }} / 5</span>
      </div>
      <div class="evaluation-card__score">
        <span class="evaluation-card__label">服务体验</span>
        <el-rate :model-value="evaluation.serviceScore" disabled :size="16" />
        <span class="evaluation-card__value">{{ evaluation.serviceScore }} / 5</span>
      </div>
    </div>
    <div class="evaluation-card__content">
      <span class="evaluation-card__label">评价内容</span>
      <p>{{ evaluation.content || '用户未填写文字评价' }}</p>
    </div>
    <div class="evaluation-card__footer">评价时间：{{ evaluatedTimeText }}</div>
  </div>
  <div v-else class="evaluation-card__empty" :class="{ 'evaluation-card--plain': !bordered }">
    {{ emptyText }}
  </div>
</template>

<style scoped>
.evaluation-card,
.evaluation-card__empty {
  padding: 18px 20px;
  border: 1px solid #e0eaf2;
  border-radius: 14px;
  background: #fff;
  color: #5b7186;
  line-height: 1.7;
}
.evaluation-card--plain { border-color: transparent; background: transparent; padding-left: 0; padding-right: 0; }
.evaluation-card__scores { display: flex; flex-wrap: wrap; gap: 28px; margin-bottom: 14px; }
.evaluation-card__score { display: flex; align-items: center; gap: 10px; }
.evaluation-card__label { color: #899aa8; font-size: 12px; font-weight: 600; }
.evaluation-card__value { color: #294e6c; font-size: 13px; font-weight: 600; }
.evaluation-card__content { display: grid; gap: 6px; }
.evaluation-card__content p { margin: 0; color: #294e6c; font-size: 14px; overflow-wrap: anywhere; }
.evaluation-card__footer { margin-top: 12px; color: #96a5b1; font-size: 12px; }
.evaluation-card__empty { color: #899aa8; font-size: 13px; text-align: center; }
</style>
