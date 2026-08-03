<script setup lang="ts">
import type { UserRole } from '@/store/user'

const props = defineProps<{ role: UserRole }>()

const roleLabels: Record<UserRole, string> = {
  USER: '学生用户端',
  COURIER: '配送员端',
  ADMIN: '管理员后台',
}
</script>

<template>
  <main class="phase-shell">
    <section class="phase-card">
      <span class="phase-card__eyebrow">{{ props.role }}</span>
      <h1>{{ roleLabels[props.role] }}</h1>
      <p>角色路由与权限元数据已配置，正式业务界面将在对应 Step 实现。</p>
    </section>
  </main>
</template>
