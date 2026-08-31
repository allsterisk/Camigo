<script setup lang="ts">
import { shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AdminExceptionResolutionDialog from '@/components/admin/AdminExceptionResolutionDialog.vue'
import AdminExceptionTable from '@/components/admin/AdminExceptionTable.vue'
import AdminFilters from '@/components/admin/AdminFilters.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAdminExceptions } from '@/composables/useAdminExceptions'
import { useAdminListQuery } from '@/composables/useAdminListQuery'
import type { ParcelException, ParcelExceptionResolution, ParcelExceptionStatus } from '@/types/parcel-exception'

const options = [{ label: '待处理', value: 'PENDING' }, { label: '已解决', value: 'RESOLVED' }, { label: '已退回', value: 'RETURNED' }]
const { route, keyword, status, page, search, reset, setPage } = useAdminListQuery('status')
const { records, total, loading, actionId, errorMessage, load, resolve } = useAdminExceptions()
const selected = shallowRef<ParcelException | null>(null)
const dialogVisible = shallowRef(false)

function refresh() {
  return load({ page: page.value, pageSize: 10, keyword: String(route.query.keyword || ''), status: (route.query.status || '') as ParcelExceptionStatus | '' })
}
function openResolution(record: ParcelException) { selected.value = record; dialogVisible.value = true }
function closeResolution() { if (actionId.value === null) selected.value = null }
async function handleResolution(type: ParcelExceptionResolution, note: string) {
  if (!selected.value || !(await resolve(selected.value.id, type, note))) return
  dialogVisible.value = false; ElMessage.success('异常处理已完成，状态、容量和路线已同步'); await refresh()
}
watch(() => route.query, refresh, { immediate: true })
</script>

<template>
  <div>
    <PageHeader title="异常处理" description="核实配送现场异常，恢复配送、恢复分拣或确认退回；每次处理都会留下完整记录。" refreshable :refreshing="loading" @refresh="refresh" />
    <AdminFilters v-model:keyword="keyword" v-model:status="status" placeholder="搜索快递单号、配送单号或配送员" :options="options" :loading="loading" @search="search" @reset="reset" />
    <el-alert v-if="errorMessage && !dialogVisible" class="admin-exceptions__alert" :title="errorMessage" type="error" :closable="false" show-icon />
    <section class="admin-exceptions__surface">
      <AdminExceptionTable :records="records" :loading="loading" :action-id="actionId" @resolve="openResolution" />
      <el-pagination v-if="total > 10" class="admin-exceptions__pagination" background layout="prev, pager, next, total" :current-page="page" :page-size="10" :total="total" @current-change="setPage" />
    </section>
    <AdminExceptionResolutionDialog v-model="dialogVisible" :record="selected" :submitting="actionId !== null" :error-message="dialogVisible ? errorMessage : ''" @submit="handleResolution" @closed="closeResolution" />
  </div>
</template>

<style scoped>
.admin-exceptions__alert { margin-bottom: 18px; }
.admin-exceptions__surface { min-height: 320px; padding: 20px; border: 1px solid #dce8e7; border-radius: 16px; background: #fff; box-shadow: 0 10px 28px rgb(12 71 68 / 5%); }
.admin-exceptions__pagination { justify-content: flex-end; margin-top: 22px; }
@media (max-width: 720px) { .admin-exceptions__surface { padding: 12px; overflow-x: auto; } }
</style>
