<script setup lang="ts">
import { Box, DataAnalysis, OfficeBuilding, User, Van } from '@element-plus/icons-vue'
import AppRoleLayout from '@/components/layout/AppRoleLayout.vue'

const menuItems = [
  { path: '/admin', label: '运营概览', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/parcels', label: '快递管理', icon: Box },
  { path: '/admin/couriers', label: '配送员管理', icon: Van },
  { path: '/admin/lockers', label: '快递柜管理', icon: OfficeBuilding },
] as const
</script>

<template>
  <AppRoleLayout
    home-path="/admin"
    role-label="管理员工作台"
    headline="全局可见，稳健运营"
    :menu-items="menuItems"
    accent="#0b6f9f"
  />
</template>
