<script setup lang="ts">
import EvaluationCard from '@/components/common/EvaluationCard.vue'
import type { Evaluation } from '@/types/evaluation'

const visible = defineModel<boolean>({ required: true })
withDefaults(defineProps<{
  title?: string
  evaluations: readonly Evaluation[]
  loading?: boolean
  errorMessage?: string
  emptyText?: string
}>(), {
  title: '查看评价',
  loading: false,
  errorMessage: '',
  emptyText: '该配送订单暂无评价',
})
const emit = defineEmits<{ closed: [] }>()
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(560px, calc(100vw - 32px))"
    :close-on-click-modal="!loading"
    @closed="emit('closed')"
  >
    <el-alert
      v-if="errorMessage"
      class="evaluation-dialog__alert"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <div v-loading="loading" class="evaluation-dialog__body">
      <template v-if="!loading && evaluations.length > 0">
        <EvaluationCard
          v-for="evaluation in evaluations"
          :key="evaluation.id"
          :evaluation="evaluation"
          :bordered="false"
          class="evaluation-dialog__item"
        />
      </template>
      <el-empty
        v-else-if="!loading && !errorMessage"
        :description="emptyText"
        :image-size="88"
      />
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.evaluation-dialog__alert { margin-bottom: 14px; }
.evaluation-dialog__body { min-height: 120px; }
.evaluation-dialog__item + .evaluation-dialog__item {
  margin-top: 12px;
  padding-top: 14px;
  border-top: 1px dashed #e0eaf2;
}
</style>
