<script setup lang="ts">
import { computed, type Component } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import BrandMark from '@/components/brand/BrandMark.vue'
import { useUserStore } from '@/store/user'

interface MenuItem {
  path: string
  label: string
  icon: string | Component
}

const props = withDefaults(defineProps<{
  homePath: string
  roleLabel: string
  headline: string
  menuItems: readonly MenuItem[]
  accent?: string
}>(), {
  accent: '#0b6b9f',
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { displayName, userInfo } = storeToRefs(userStore)

const activeMenu = computed(() => (
  [...props.menuItems]
    .sort((left, right) => right.path.length - left.path.length)
    .find((item) => route.path === item.path || route.path.startsWith(`${item.path}/`))
    ?.path ?? props.homePath
))

async function handleLogout() {
  userStore.logout()
  await router.replace({ name: 'login' })
  ElMessage.success('已安全退出登录')
}
</script>

<template>
  <div class="role-layout" :style="{ '--role-accent': accent }">
    <aside class="role-layout__sidebar">
      <RouterLink class="role-layout__brand" :to="homePath">
        <BrandMark :size="46" />
        <span>
          <strong>校速达</strong>
          <small>CAMIGO</small>
        </span>
      </RouterLink>

      <el-menu class="role-layout__menu" :default-active="activeMenu" router>
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span class="role-layout__menu-icon">
            <component :is="item.icon" v-if="typeof item.icon !== 'string'" />
            <template v-else>{{ item.icon }}</template>
          </span>{{ item.label }}
        </el-menu-item>
      </el-menu>

      <div class="role-layout__account">
        <div class="role-layout__avatar">{{ displayName.slice(0, 1) }}</div>
        <span class="role-layout__account-copy">
          <strong>{{ displayName }}</strong>
          <small>{{ userInfo?.studentNo || userInfo?.username }}</small>
        </span>
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </aside>

    <section class="role-layout__workspace">
      <header class="role-layout__topbar">
        <div class="role-layout__topbar-copy">
          <span class="role-layout__topbar-label">{{ roleLabel }}</span>
          <strong>{{ headline }}</strong>
        </div>
        <span class="role-layout__status"><i /> 服务运行中</span>
      </header>
      <div class="role-layout__content">
        <RouterView />
      </div>
    </section>
  </div>
</template>

<style scoped>
.role-layout { min-height: 100vh; display: grid; grid-template-columns: 248px minmax(0, 1fr); background: #f3f7fa; }
.role-layout__sidebar { position: sticky; top: 0; height: 100vh; display: flex; flex-direction: column; padding: 28px 20px 20px; border-right: 1px solid #deebf2; background: #fff; }
.role-layout__brand { display: flex; align-items: center; gap: 13px; padding: 0 10px 28px; color: inherit; text-decoration: none; }
.role-layout__brand strong, .role-layout__brand small { display: block; }
.role-layout__brand strong { color: #0a416f; font-size: 21px; letter-spacing: .08em; }
.role-layout__brand small { margin-top: 2px; color: #7990a4; font-size: 9px; letter-spacing: .25em; }
.role-layout__menu { flex: 1; border-right: 0; }
.role-layout__menu :deep(.el-menu-item) { height: 50px; margin: 6px 0; border-radius: 11px; color: #587086; }
.role-layout__menu :deep(.el-menu-item.is-active) { color: var(--role-accent); background: color-mix(in srgb, var(--role-accent) 10%, white); font-weight: 700; }
.role-layout__menu-icon { display: grid; width: 28px; height: 28px; margin-right: 10px; place-items: center; border-radius: 8px; color: var(--role-accent); background: color-mix(in srgb, var(--role-accent) 9%, white); font-size: 11px; font-weight: 800; }
.role-layout__menu-icon :deep(svg) { width: 15px; height: 15px; }
.role-layout__account { display: flex; align-items: center; gap: 10px; padding: 14px 10px 4px; border-top: 1px solid #edf2f6; }
.role-layout__avatar { display: grid; flex: 0 0 36px; height: 36px; place-items: center; border-radius: 11px; color: #fff; background: var(--role-accent); font-weight: 800; }
.role-layout__account-copy { min-width: 0; flex: 1; }
.role-layout__account-copy strong, .role-layout__account-copy small { overflow: hidden; display: block; text-overflow: ellipsis; white-space: nowrap; }
.role-layout__account-copy strong { color: #274c6b; font-size: 13px; }
.role-layout__account-copy small { margin-top: 3px; color: #96a5b2; font-size: 10px; }
.role-layout__workspace { min-width: 0; }
.role-layout__topbar { height: 74px; display: flex; align-items: center; justify-content: space-between; padding: 0 36px; border-bottom: 1px solid #deebf2; background: rgb(255 255 255 / 86%); backdrop-filter: blur(14px); }
.role-layout__topbar-copy { display: flex; flex-direction: column; gap: 3px; }
.role-layout__topbar-label { color: #8a9cab; font-size: 10px; letter-spacing: .15em; }
.role-layout__topbar strong { color: #294e6c; font-size: 14px; }
.role-layout__status { color: #668095; font-size: 12px; }
.role-layout__status i { display: inline-block; width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; background: #28ad72; box-shadow: 0 0 0 4px rgb(40 173 114 / 12%); }
.role-layout__content { max-width: 1280px; margin: 0 auto; padding: 36px; }

@media (max-width: 760px) {
  .role-layout { grid-template-columns: 1fr; }
  .role-layout__sidebar { position: static; width: 100%; height: auto; padding: 18px; }
  .role-layout__brand { padding-bottom: 14px; }
  .role-layout__menu { display: flex; overflow-x: auto; }
  .role-layout__menu :deep(.el-menu-item) { flex: 0 0 auto; padding: 0 14px; }
  .role-layout__account { display: none; }
  .role-layout__topbar { height: 60px; padding: 0 20px; }
  .role-layout__status { display: none; }
  .role-layout__content { padding: 24px 18px; }
}
</style>
