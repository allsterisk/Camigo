<script setup lang="ts">
import type { AdminUser, UserStatus } from '@/types/admin'
import { formatDateTime } from '@/utils/parcel'
defineProps<{ users: readonly AdminUser[]; loading: boolean; actionId: number | null }>()
defineEmits<{ statusChange: [id: number, status: UserStatus]; remove: [user: AdminUser] }>()
const statuses: { label: string; value: UserStatus }[] = [
  { label: '正常', value: 'ACTIVE' }, { label: '停用', value: 'DISABLED' },
  { label: '锁定', value: 'LOCKED' }, { label: '已注销', value: 'CANCELLED' },
]
</script>
<template>
  <el-table :data="users" v-loading="loading" row-key="id" stripe>
    <el-table-column label="用户" min-width="150"><template #default="{ row }"><strong>{{ row.realName }}</strong><br><small>@{{ row.username }}</small></template></el-table-column>
    <el-table-column label="联系方式" prop="phone" min-width="130" />
    <el-table-column label="学号" min-width="120"><template #default="{ row }">{{ row.studentNo || '—' }}</template></el-table-column>
    <el-table-column label="角色" min-width="150"><template #default="{ row }"><el-tag v-for="role in row.roles" :key="role" class="admin-table__tag" effect="plain">{{ role }}</el-tag></template></el-table-column>
    <el-table-column label="注册时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
    <el-table-column label="状态" min-width="130"><template #default="{ row }"><el-select :model-value="row.status" :disabled="actionId !== null" @change="$emit('statusChange', row.id, $event)"><el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></template></el-table-column>
    <el-table-column label="操作" fixed="right" width="90"><template #default="{ row }"><el-button text type="danger" :loading="actionId === row.id" @click="$emit('remove', row)">删除</el-button></template></el-table-column>
  </el-table>
</template>
<style scoped>.admin-table__tag { margin: 2px 4px 2px 0; } small { color: #8695a2; }</style>
