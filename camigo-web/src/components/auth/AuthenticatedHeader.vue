<script setup lang="ts">
defineProps<{
  roleLabel: string
  userName: string
}>()

defineEmits<{ logout: [] }>()
</script>

<template>
  <header class="auth-header">
    <div>
      <strong class="auth-header__brand">校速达</strong>
      <span class="auth-header__role">{{ roleLabel }}</span>
    </div>
    <div class="auth-header__user">
      <span>{{ userName }}</span>
      <el-button plain @click="$emit('logout')">退出登录</el-button>
    </div>
  </header>
</template>

<style scoped>
.auth-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.auth-header__brand { color: #0b416d; font-size: 22px; }
.auth-header__role { margin-left: 12px; color: #71879a; font-size: 13px; }
.auth-header__user { display: flex; align-items: center; gap: 14px; color: #47647d; }

@media (max-width: 560px) {
  .auth-header { align-items: flex-start; }
  .auth-header__user { flex-direction: column; align-items: flex-end; gap: 8px; }
}
</style>
