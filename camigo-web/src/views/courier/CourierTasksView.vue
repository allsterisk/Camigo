<script setup lang="ts">
import { computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import CourierTaskTable from '@/components/courier/CourierTaskTable.vue'
import CourierRoutePanel from '@/components/courier/CourierRoutePanel.vue'
import CourierStorageDialog from '@/components/courier/CourierStorageDialog.vue'
import CourierExceptionDialog from '@/components/courier/CourierExceptionDialog.vue'
import CourierDispatchOfferCard from '@/components/courier/CourierDispatchOfferCard.vue'
import { useCourierTasks } from '@/composables/useCourierTasks'
import { useCourierRoute } from '@/composables/useCourierRoute'
import { useCourierStorage } from '@/composables/useCourierStorage'
import { useCourierExceptions } from '@/composables/useCourierExceptions'
import { useCourierDispatchOffer } from '@/composables/useCourierDispatchOffer'
import type { ParcelExceptionType } from '@/types/parcel-exception'
import type { DispatchRejectReason } from '@/types/dispatch'
import { stationPickupOrderIds } from '@/utils/courier-route'

const route = useRoute()
const router = useRouter()
const {
  tasks,
  total,
  pageSize,
  loading,
  errorMessage,
  load,
} = useCourierTasks()
const {
  route: currentRoute,
  loading: routeLoading,
  actionOrderId,
  actionBusy,
  errorMessage: routeError,
  load: loadRoute,
  confirmPickup,
  confirmPickupBatch,
  confirmArrival,
} = useCourierRoute()
const {
  visible: storageVisible,
  orderId: storageOrderId,
  slots: storageSlots,
  loading: storageLoading,
  submitting: storageSubmitting,
  errorMessage: storageError,
  open: openStorage,
  close: closeStorage,
  store,
} = useCourierStorage()
const {
  visible: exceptionVisible, orderId: exceptionOrderId, submitting: exceptionSubmitting,
  errorMessage: exceptionError, open: openException, close: closeException, submit: submitException,
} = useCourierExceptions()
const {
  offers: dispatchOffers, loading: offerLoading, submittingAssignmentId,
  errorMessage: offerError, remainingSecondsById, load: loadOffer,
  accept: acceptOffer, reject: rejectOffer,
} = useCourierDispatchOffer()

const currentPage = computed(() => parsePage(route.query.page))
const activeActionOrderId = computed(() => {
  if (actionBusy.value) return actionOrderId.value ?? -1
  return actionOrderId.value ?? storageOrderId.value
    ?? (exceptionSubmitting.value ? exceptionOrderId.value : null)
})
const stationPickupIds = computed(() => stationPickupOrderIds(currentRoute.value))
const actionablePickupIds = computed(() => new Set(stationPickupIds.value))
const actionableArrivalIds = computed(() => {
  const step = currentRoute.value?.steps?.[0]
  if (!step) return new Set<number>()
  return new Set(step.dropoffOrderIds ?? [])
})

function parsePage(value: unknown): number {
  const raw = Array.isArray(value) ? value[0] : value
  const page = Number.parseInt(typeof raw === 'string' ? raw : '1', 10)
  return Number.isInteger(page) && page > 0 ? page : 1
}

function refresh() {
  return Promise.all([
    load({ page: currentPage.value, pageSize: 10, status: 'PROCESSING' }),
    loadRoute(),
    loadOffer(),
  ])
}

async function handleAcceptOffer(assignmentId: number) {
  if (!(await acceptOffer(assignmentId))) return
  ElMessage.success('派单已接受，新的综合路线已经生成')
  await refresh()
}

async function handleRejectOffer(assignmentId: number, reasonCode: DispatchRejectReason,
                                 reasonDetail?: string) {
  if (!(await rejectOffer(assignmentId, reasonCode, reasonDetail))) return
  ElMessage.success('已记录拒绝原因，系统将邀请下一位候选配送员')
  await refresh()
}

function setPage(page: number) {
  void router.replace({
    query: { page: page === 1 ? undefined : String(page) },
  })
}

async function handlePickup(orderId: number) {
  try {
    await ElMessageBox.confirm(
      '请确认你已在该快递站取到该包裹。',
      '确认取件',
      { type: 'warning', confirmButtonText: '已取到包裹', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  const result = await confirmPickup(orderId)
  if (!result) return
  ElMessage.success(`取件已确认，当前位置更新为${result.currentLocationName}`)
  await load({ page: currentPage.value, pageSize: 10, status: 'PROCESSING' })
}

async function handleBatchPickup(orderIds: number[]) {
  try {
    await ElMessageBox.confirm(
      `请确认你已在当前服务站一次性取到这 ${orderIds.length} 件包裹。系统将批量确认取件并刷新剩余配送路线。`,
      '本站全部取件',
      { type: 'warning', confirmButtonText: '已全部取到', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  const result = await confirmPickupBatch(orderIds)
  if (!result) return
  ElMessage.success(
    `已在${result.pickupLocationName}确认取件 ${result.pickedCount} 件，剩余路线已更新`,
  )
  await load({ page: currentPage.value, pageSize: 10, status: 'PROCESSING' })
}

async function handleArrival(orderId: number) {
  try {
    await ElMessageBox.confirm(
      '请确认你已到达该订单的配送目的地。入柜和完成订单将在后续步骤处理。',
      '确认到达',
      { type: 'warning', confirmButtonText: '已到达目的地', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  const result = await confirmArrival(orderId)
  if (!result) return
  ElMessage.success(`到达已确认，当前位置更新为${result.currentLocationName}`)
  await load({ page: currentPage.value, pageSize: 10, status: 'PROCESSING' })
}

function handleStorage(orderId: number) {
  void openStorage(orderId)
}

async function handleStore(lockerSlotId: number) {
  const result = await store(lockerSlotId)
  if (!result) return
  ElMessage.success(`已存入格口 ${result.slotNo}，学生取件码已生成`)
  await refresh()
}

async function handleException(type: ParcelExceptionType, description: string) {
  if (!(await submitException(type, description))) return
  ElMessage.success('异常已上报，任务已暂停并等待管理员处理')
  await refresh()
}

watch(currentPage, refresh, { immediate: true })
</script>

<template>
  <div>
    <PageHeader
      title="我的任务"
      description="同站多件可一次取齐；取件、到达后路线会从新位置自动刷新。"
      refreshable
      :refreshing="loading"
      @refresh="refresh"
    />
    <CourierDispatchOfferCard
      :offers="dispatchOffers"
      :remaining-seconds-by-id="remainingSecondsById"
      :loading="offerLoading"
      :submitting-assignment-id="submittingAssignmentId"
      :error-message="offerError"
      @refresh="loadOffer"
      @accept="handleAcceptOffer"
      @reject="handleRejectOffer"
    />
    <CourierRoutePanel
      class="courier-tasks__route"
      :route="currentRoute"
      :loading="routeLoading"
      :error-message="routeError"
      :batch-acting="actionBusy"
      :station-pickup-ids="stationPickupIds"
      @refresh="loadRoute"
      @batch-pickup="handleBatchPickup"
    />
    <el-alert
      v-if="errorMessage"
      class="courier-tasks__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <section class="courier-tasks__table-card" aria-labelledby="active-task-title">
      <div class="courier-tasks__section-heading">
        <div>
          <h2 id="active-task-title">执行中任务</h2>
          <p>人在快递站即可取该站待取件；送达仍按路线下一站操作。同站多件可用「本站全部取件」。</p>
        </div>
        <span>{{ total }} 项</span>
      </div>
      <EmptyState
        v-if="!loading && tasks.length === 0"
        title="暂无执行中任务"
        description="接受上方派单后，任务会进入这里并生成动态路线。"
        action-label="刷新任务"
        @action="refresh"
      />
      <template v-else>
        <CourierTaskTable
          :tasks="tasks"
          :loading="loading"
          :action-order-id="activeActionOrderId"
          :actionable-pickup-ids="actionablePickupIds"
          :actionable-arrival-ids="actionableArrivalIds"
          show-actions
          @pickup="handlePickup"
          @arrival="handleArrival"
          @storage="handleStorage"
          @exception="openException"
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
    <CourierStorageDialog
      v-model="storageVisible"
      :slots="storageSlots"
      :loading="storageLoading"
      :submitting="storageSubmitting"
      :error-message="storageError"
      @confirm="handleStore"
      @closed="closeStorage"
    />
    <CourierExceptionDialog v-model="exceptionVisible" :order-id="exceptionOrderId"
      :submitting="exceptionSubmitting" :error-message="exceptionError"
      @submit="handleException" @closed="closeException" />
  </div>
</template>

<style scoped>
.courier-tasks__error { margin-bottom: 18px; }
.courier-tasks__route { margin-bottom: 22px; }
.courier-tasks__table-card { min-height: 280px; padding: 20px; border: 1px solid #dfeae9; border-radius: 18px; background: #fff; box-shadow: 0 10px 28px rgb(12 71 68 / 5%); }
.courier-tasks__section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.courier-tasks__section-heading h2 { margin: 0; color: #204d49; font-size: 20px; }
.courier-tasks__section-heading p { margin: 6px 0 0; color: #6d8581; font-size: 13px; line-height: 1.6; }
.courier-tasks__section-heading span { flex: none; color: #6d8581; font-size: 13px; }
.courier-tasks__pagination { justify-content: flex-end; margin-top: 22px; }
</style>
