<script setup lang="ts">
import { computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import CourierTaskFilters from '@/components/courier/CourierTaskFilters.vue'
import CourierTaskTable from '@/components/courier/CourierTaskTable.vue'
import { useCourierTasks } from '@/composables/useCourierTasks'
import type { DeliveryOrderStatus } from '@/types/delivery-order'

type TaskStatus = Extract<DeliveryOrderStatus, 'WAITING' | 'PROCESSING'>

const route = useRoute()
const router = useRouter()
const {
  tasks,
  total,
  pageSize,
  loading,
  actionOrderId,
  errorMessage,
  load,
  accept,
  complete,
} = useCourierTasks()

const status = computed<TaskStatus>({
  get: () => route.query.status === 'PROCESSING' ? 'PROCESSING' : 'WAITING',
  set: (value) => {
    void router.replace({
      query: { ...route.query, status: value === 'WAITING' ? undefined : value, page: undefined },
    })
  },
})
const currentPage = computed(() => parsePage(route.query.page))

function parsePage(value: unknown): number {
  const raw = Array.isArray(value) ? value[0] : value
  const page = Number.parseInt(typeof raw === 'string' ? raw : '1', 10)
  return Number.isInteger(page) && page > 0 ? page : 1
}

function refresh() {
  return load({ page: currentPage.value, pageSize: 10, status: status.value })
}

function setPage(page: number) {
  void router.replace({
    query: { ...route.query, page: page === 1 ? undefined : String(page) },
  })
}

async function handleAccept(orderId: number) {
  if (!await accept(orderId)) return
  ElMessage.success('接单成功，任务已进入配送中')
  status.value = 'PROCESSING'
}

async function handleComplete(orderId: number) {
  try {
    await ElMessageBox.confirm(
      '确认该任务已经送达？提交后将进入配送记录。',
      '确认送达',
      { type: 'warning', confirmButtonText: '确认送达', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  if (!await complete(orderId)) return
  ElMessage.success('配送状态已更新为已送达')
  await refresh()
}

watch([status, currentPage], refresh, { immediate: true })
</script>

<template>
  <div>
    <PageHeader
      eyebrow="DELIVERY TASKS"
      title="我的任务"
      description="查看校园可接订单，接单后跟进配送并确认送达。"
      refreshable
      :refreshing="loading"
      @refresh="refresh"
    />
    <CourierTaskFilters v-model:status="status" :loading="loading" />
    <el-alert
      v-if="errorMessage"
      class="courier-tasks__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <section class="courier-tasks__table-card">
      <EmptyState
        v-if="!loading && tasks.length === 0"
        :title="status === 'WAITING' ? '暂无可接任务' : '暂无进行中任务'"
        :description="status === 'WAITING' ? '新配送订单出现后会显示在这里。' : '接单成功后会进入进行中列表。'"
        action-label="刷新任务"
        @action="refresh"
      />
      <template v-else>
        <CourierTaskTable
          :tasks="tasks"
          :loading="loading"
          :action-order-id="actionOrderId"
          show-actions
          @accept="handleAccept"
          @complete="handleComplete"
        />
        <el-pagination
          v-if="total > pageSize"
          class="courier-tasks__pagination"
          background
          layout="prev, pager, next, total"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="setPage"
        />
      </template>
    </section>
  </div>
</template>

<style scoped>
.courier-tasks__error { margin-bottom: 18px; }
.courier-tasks__table-card { min-height: 280px; padding: 20px; border: 1px solid #dfeae9; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgb(12 71 68 / 5%); }
.courier-tasks__pagination { justify-content: flex-end; margin-top: 22px; }
</style>
