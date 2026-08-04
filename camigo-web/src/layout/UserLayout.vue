<script setup lang="ts">
import AppRoleLayout from '@/components/layout/AppRoleLayout.vue'

const menuItems = [
  { path: '/user', label: '学生首页', icon: '首' },
  { path: '/user/parcels', label: '我的快递', icon: '件' },
  { path: '/user/evaluation', label: '配送评价', icon: '评' },
] as const
</script>

<template>
  <AppRoleLayout
    home-path="/user"
    role-label="学生服务中心"
    headline="校园快递，一站掌握"
    :menu-items="menuItems"
  />
</template>
