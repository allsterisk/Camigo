<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EvaluationDetailDialog from '@/components/common/EvaluationDetailDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import CourierTaskTable from '@/components/courier/CourierTaskTable.vue'
import { useCourierTasks } from '@/composables/useCourierTasks'
import { useEvaluationViewer } from '@/composables/useEvaluationViewer'

const route = useRoute()
const router = useRouter()
const { tasks, total, pageSize, loading, errorMessage, load } = useCourierTasks()
const {
  visible: evaluationVisible, loading: evaluationLoading,
  errorMessage: evaluationError, evaluations,
  open: openEvaluation, close: closeEvaluation,
} = useEvaluationViewer('order')
const currentPage = computed(() => parsePage(route.query.page))

function parsePage(value: unknown): number {
  const raw = Array.isArray(value) ? value[0] : value
  const page = Number.parseInt(typeof raw === 'string' ? raw : '1', 10)
  return Number.isInteger(page) && page > 0 ? page : 1
}

function refresh() {
  return load({ page: currentPage.value, pageSize: 10, status: 'DELIVERED' })
}

function setPage(page: number) {
  void router.replace({ query: { page: page === 1 ? undefined : String(page) } })
}

watch(currentPage, refresh, { immediate: true })
</script>

<template>
  <div>
    <PageHeader
      title="配送记录"
      description="按完成时间查看本人已经送达的历史配送订单。"
      refreshable
      :refreshing="loading"
      @refresh="refresh"
    />
    <el-alert
      v-if="errorMessage"
      class="courier-history__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <section class="courier-history__table-card">
      <EmptyState
        v-if="!loading && tasks.length === 0"
        title="暂无配送记录"
        description="确认任务送达后，历史记录会显示在这里。"
        action-label="刷新记录"
        @action="refresh"
      />
      <template v-else>
        <CourierTaskTable
          :tasks="tasks"
          :loading="loading"
          show-actions
          show-evaluation
          time-column-label="完成时间"
          @evaluation="openEvaluation"
        />
        <el-pagination
          v-if="total > pageSize"
          class="courier-history__pagination"
          background
          layout="prev, pager, next, total"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="setPage"
        />
      </template>
    </section>
    <EvaluationDetailDialog
      v-model="evaluationVisible"
      title="配送评价"
      :evaluations="evaluations"
      :loading="evaluationLoading"
      :error-message="evaluationError"
      empty-text="该配送订单暂无评价"
      @closed="closeEvaluation"
    />
  </div>
</template>

<style scoped>
.courier-history__error { margin-bottom: 18px; }
.courier-history__table-card { min-height: 280px; padding: 20px; border: 1px solid #dfeae9; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgb(12 71 68 / 5%); }
.courier-history__pagination { justify-content: flex-end; margin-top: 22px; }
</style>
