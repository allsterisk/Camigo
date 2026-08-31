<script setup lang="ts">
import { watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminDispatchDetailDialog from '@/components/admin/AdminDispatchDetailDialog.vue'
import AdminFilters from '@/components/admin/AdminFilters.vue'
import AdminParcelTable from '@/components/admin/AdminParcelTable.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAdminDispatchDetails } from '@/composables/useAdminDispatchDetails'
import { useAdminListQuery } from '@/composables/useAdminListQuery'
import { useAdminParcels } from '@/composables/useAdminParcels'
import type { ParcelStatus } from '@/types/parcel'

const filterOptions = [
  { label: '已到校', value: 'ARRIVED' }, { label: '配送中', value: 'DELIVERING' },
  { label: '待取件', value: 'STORED' }, { label: '已取件', value: 'PICKED' },
  { label: '已完成', value: 'COMPLETED' },
]
const { route, keyword, status, page, search, reset, setPage } = useAdminListQuery('status')
const {
  records, total, loading, actionId, errorMessage, load, changeStatus, startDelivery,
} = useAdminParcels()
const {
  visible: dispatchVisible, parcelId: dispatchParcelId, records: dispatchRecords,
  loading: dispatchLoading, errorMessage: dispatchError, open: openDispatch, close: closeDispatch,
} = useAdminDispatchDetails()

function refresh() {
  return load({
    page: page.value, pageSize: 10, trackingNo: String(route.query.keyword || ''),
    status: (route.query.status || '') as ParcelStatus | '',
  })
}
async function handleStatus(id: number, value: string) {
  try {
    await ElMessageBox.confirm('确认更新该快递状态？', '状态更新', { type: 'warning' })
  } catch { return }
  if (await changeStatus(id, value)) {
    ElMessage.success('快递状态已更新')
    await refresh()
  }
}
async function handleStartDelivery(id: number) {
  try {
    await ElMessageBox.confirm(
      '确认开始新一轮智能派单？系统会重新评估当前可用配送员。',
      '开始配送',
      { type: 'warning', confirmButtonText: '开始配送', cancelButtonText: '取消' },
    )
  } catch { return }
  if (await startDelivery(id)) {
    ElMessage.success('新一轮智能派单已启动')
    await refresh()
  }
}
watch(() => route.query, refresh, { immediate: true })
</script>

<template>
  <div>
    <PageHeader title="快递管理" description="查看全平台快递；派单候选全部拒绝或超时后，由管理员决定何时开始下一轮。" refreshable :refreshing="loading" @refresh="refresh" />
    <AdminFilters v-model:keyword="keyword" v-model:status="status" placeholder="输入快递单号" :options="filterOptions" :loading="loading" @search="search" @reset="reset" />
    <el-alert v-if="errorMessage" class="admin-parcels__alert" :title="errorMessage" type="error" :closable="false" show-icon />
    <section class="admin-parcels__surface">
      <AdminParcelTable :parcels="records" :loading="loading" :action-id="actionId" @status-change="handleStatus" @start-delivery="handleStartDelivery" @dispatch-details="openDispatch" />
      <el-pagination v-if="total > 10" class="admin-parcels__pagination" background layout="prev, pager, next, total" :current-page="page" :page-size="10" :total="total" @current-change="setPage" />
    </section>
    <AdminDispatchDetailDialog v-model="dispatchVisible" :parcel-id="dispatchParcelId" :records="dispatchRecords" :loading="dispatchLoading" :error-message="dispatchError" @closed="closeDispatch" />
  </div>
</template>

<style scoped>
.admin-parcels__alert { margin-bottom: 18px; }
.admin-parcels__surface { min-height: 320px; padding: 20px; border: 1px solid #dce8e7; border-radius: 16px; background: #fff; box-shadow: 0 10px 28px rgb(12 71 68 / 5%); }
.admin-parcels__pagination { justify-content: flex-end; margin-top: 22px; }
@media (max-width: 720px) { .admin-parcels__surface { padding: 12px; overflow-x: auto; } }
</style>
