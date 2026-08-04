<script setup lang="ts">
import { watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminFilters from '@/components/admin/AdminFilters.vue'
import AdminUserTable from '@/components/admin/AdminUserTable.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAdminListQuery } from '@/composables/useAdminListQuery'
import { useAdminUsers } from '@/composables/useAdminUsers'
import type { AdminUser, UserStatus } from '@/types/admin'
const options = [{ label: '正常', value: 'ACTIVE' }, { label: '停用', value: 'DISABLED' }, { label: '锁定', value: 'LOCKED' }, { label: '已注销', value: 'CANCELLED' }]
const { route, keyword, status, page, search, reset, setPage } = useAdminListQuery('status')
const { records, total, loading, actionId, errorMessage, load, changeStatus, remove } = useAdminUsers()
function refresh() { return load({ page: page.value, pageSize: 10, keyword: String(route.query.keyword || ''), status: (route.query.status || '') as UserStatus | '' }) }
async function handleStatus(id: number, value: UserStatus) { if (await changeStatus(id, value)) { ElMessage.success('用户状态已更新'); await refresh() } }
async function handleRemove(user: AdminUser) { try { await ElMessageBox.confirm(`确认删除用户“${user.realName}”？已有业务数据的账号将被保护。`, '删除用户', { type: 'warning' }) } catch { return }; if (await remove(user.id)) { ElMessage.success('用户已删除'); await refresh() } }
watch(() => route.query, refresh, { immediate: true })
</script>
<template><div><PageHeader eyebrow="USER MANAGEMENT" title="用户管理" description="查询用户、调整账号状态，并安全删除无业务数据的账号。" refreshable :refreshing="loading" @refresh="refresh" /><AdminFilters v-model:keyword="keyword" v-model:status="status" placeholder="用户名、姓名、手机号或学号" :options="options" :loading="loading" @search="search" @reset="reset" /><el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon /><section class="admin-list-card"><AdminUserTable :users="records" :loading="loading" :action-id="actionId" @status-change="handleStatus" @remove="handleRemove" /><el-pagination v-if="total > 10" background layout="prev, pager, next, total" :current-page="page" :page-size="10" :total="total" @current-change="setPage" /></section></div></template>
