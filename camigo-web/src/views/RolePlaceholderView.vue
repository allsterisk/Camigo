<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import AuthenticatedHeader from '@/components/auth/AuthenticatedHeader.vue'
import { useUserStore } from '@/store/user'
import type { UserRole } from '@/types/auth'

const props = defineProps<{ role: UserRole }>()
const router = useRouter()
const userStore = useUserStore()
const { displayName } = storeToRefs(userStore)

const roleLabels: Record<UserRole, string> = {
  USER: '学生用户端',
  COURIER: '配送员端',
  ADMIN: '管理员后台',
}

async function handleLogout() {
  userStore.logout()
  await router.replace({ name: 'login' })
  ElMessage.success('已安全退出登录')
}
</script>

<template>
  <main class="phase-shell">
    <section class="phase-card phase-card--wide">
      <AuthenticatedHeader
        :role-label="roleLabels[props.role]"
        :user-name="displayName"
        @logout="handleLogout"
      />
      <span class="phase-card__eyebrow">{{ props.role }}</span>
      <h1>{{ roleLabels[props.role] }}</h1>
      <p>登录认证与角色权限验证已完成，正式业务界面将在对应 Step 实现。</p>
    </section>
  </main>
</template>
