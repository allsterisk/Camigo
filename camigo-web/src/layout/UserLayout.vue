<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import BrandMark from '@/components/brand/BrandMark.vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { displayName, userInfo } = storeToRefs(userStore)

const activeMenu = computed(() => {
  if (route.path.startsWith('/user/parcels')) return '/user/parcels'
  return route.path
})

async function handleLogout() {
  userStore.logout()
  await router.replace({ name: 'login' })
  ElMessage.success('已安全退出登录')
}
</script>

<template>
  <div class="user-layout">
    <aside class="user-layout__sidebar">
      <RouterLink class="user-layout__brand" to="/user">
        <BrandMark :size="46" />
        <span>
          <strong>校速达</strong>
          <small>CAMIGO</small>
        </span>
      </RouterLink>

      <el-menu class="user-layout__menu" :default-active="activeMenu" router>
        <el-menu-item index="/user"><span class="user-layout__menu-icon">首</span>学生首页</el-menu-item>
        <el-menu-item index="/user/parcels"><span class="user-layout__menu-icon">件</span>我的快递</el-menu-item>
        <el-menu-item index="/user/evaluation"><span class="user-layout__menu-icon">评</span>配送评价</el-menu-item>
      </el-menu>

      <div class="user-layout__account">
        <div class="user-layout__avatar">{{ displayName.slice(0, 1) }}</div>
        <span class="user-layout__account-copy">
          <strong>{{ displayName }}</strong>
          <small>{{ userInfo?.studentNo || userInfo?.username }}</small>
        </span>
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </aside>

    <section class="user-layout__workspace">
      <header class="user-layout__topbar">
        <div>
          <span class="user-layout__topbar-label">学生服务中心</span>
          <strong>校园快递，一站掌握</strong>
        </div>
        <span class="user-layout__status"><i /> 服务运行中</span>
      </header>
      <div class="user-layout__content">
        <RouterView />
      </div>
    </section>
  </div>
</template>

<style scoped>
.user-layout { min-height: 100vh; display: grid; grid-template-columns: 248px minmax(0, 1fr); background: #f3f7fa; }
.user-layout__sidebar { position: sticky; top: 0; height: 100vh; display: flex; flex-direction: column; padding: 28px 20px 20px; border-right: 1px solid #deebf2; background: #fff; }
.user-layout__brand { display: flex; align-items: center; gap: 13px; padding: 0 10px 28px; color: inherit; text-decoration: none; }
.user-layout__brand strong, .user-layout__brand small { display: block; }
.user-layout__brand strong { color: #0a416f; font-size: 21px; letter-spacing: .08em; }
.user-layout__brand small { margin-top: 2px; color: #7990a4; font-size: 9px; letter-spacing: .25em; }
.user-layout__menu { flex: 1; border-right: 0; }
.user-layout__menu :deep(.el-menu-item) { height: 50px; margin: 6px 0; border-radius: 11px; color: #587086; }
.user-layout__menu :deep(.el-menu-item.is-active) { color: #08649a; background: #e8f4fb; font-weight: 700; }
.user-layout__menu-icon { display: grid; width: 28px; height: 28px; margin-right: 10px; place-items: center; border-radius: 8px; color: #37779e; background: #edf6fb; font-size: 11px; font-weight: 800; }
.user-layout__account { display: flex; align-items: center; gap: 10px; padding: 14px 10px 4px; border-top: 1px solid #edf2f6; }
.user-layout__avatar { display: grid; flex: 0 0 36px; height: 36px; place-items: center; border-radius: 11px; color: #fff; background: #0b6b9f; font-weight: 800; }
.user-layout__account-copy { min-width: 0; flex: 1; }
.user-layout__account-copy strong, .user-layout__account-copy small { overflow: hidden; display: block; text-overflow: ellipsis; white-space: nowrap; }
.user-layout__account-copy strong { color: #274c6b; font-size: 13px; }
.user-layout__account-copy small { margin-top: 3px; color: #96a5b2; font-size: 10px; }
.user-layout__workspace { min-width: 0; }
.user-layout__topbar { height: 74px; display: flex; align-items: center; justify-content: space-between; padding: 0 36px; border-bottom: 1px solid #deebf2; background: rgb(255 255 255 / 86%); backdrop-filter: blur(14px); }
.user-layout__topbar div { display: flex; flex-direction: column; gap: 3px; }
.user-layout__topbar-label { color: #8a9cab; font-size: 10px; letter-spacing: .15em; }
.user-layout__topbar strong { color: #294e6c; font-size: 14px; }
.user-layout__status { color: #668095; font-size: 12px; }
.user-layout__status i { display: inline-block; width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; background: #28ad72; box-shadow: 0 0 0 4px rgb(40 173 114 / 12%); }
.user-layout__content { max-width: 1280px; margin: 0 auto; padding: 36px; }

@media (max-width: 760px) {
  .user-layout { grid-template-columns: 1fr; }
  .user-layout__sidebar { position: static; width: 100%; height: auto; padding: 18px; }
  .user-layout__brand { padding-bottom: 14px; }
  .user-layout__menu { display: flex; overflow-x: auto; }
  .user-layout__menu :deep(.el-menu-item) { flex: 0 0 auto; padding: 0 14px; }
  .user-layout__account { display: none; }
  .user-layout__topbar { height: 60px; padding: 0 20px; }
  .user-layout__status { display: none; }
  .user-layout__content { padding: 24px 18px; }
}
</style>
