<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

function logout() {
  authStore.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">C</span>
        <span>
          <strong>Camigo</strong>
          <small>校速达</small>
        </span>
      </RouterLink>

      <el-menu :default-active="activeMenu" mode="horizontal" router :ellipsis="false">
        <el-menu-item index="/">工作台</el-menu-item>
        <el-menu-item v-if="authStore.hasRole('USER')" index="/parcels">我的快递</el-menu-item>
        <el-menu-item v-if="authStore.hasRole('COURIER')" index="/delivery-tasks">配送任务</el-menu-item>
      </el-menu>

      <div class="user-actions">
        <span>{{ authStore.currentUser?.realName || authStore.currentUser?.username }}</span>
        <el-button text type="primary" @click="logout">退出登录</el-button>
      </div>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>
