<script setup lang="ts">
import AppRoleLayout from '@/components/layout/AppRoleLayout.vue'

const menuItems = [
  { path: '/admin', label: '运营概览', icon: '览' },
  { path: '/admin/users', label: '用户管理', icon: '人' },
  { path: '/admin/parcels', label: '快递管理', icon: '件' },
  { path: '/admin/couriers', label: '配送员管理', icon: '配' },
  { path: '/admin/lockers', label: '快递柜管理', icon: '柜' },
] as const
</script>

<template>
  <AppRoleLayout
    home-path="/admin"
    role-label="管理员工作台"
    headline="全局可见，稳健运营"
    :menu-items="menuItems"
    accent="#8a4baf"
  />
</template>
