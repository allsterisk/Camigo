<script setup lang="ts">
import AppRoleLayout from '@/components/layout/AppRoleLayout.vue'

const menuItems = [
  { path: '/courier', label: '配送首页', icon: '首' },
  { path: '/courier/tasks', label: '我的任务', icon: '单' },
  { path: '/courier/history', label: '配送记录', icon: '录' },
] as const
</script>

<template>
  <AppRoleLayout
    home-path="/courier"
    role-label="配送员工作台"
    headline="任务清晰，准时送达"
    :menu-items="menuItems"
    accent="#087f73"
  />
</template>
