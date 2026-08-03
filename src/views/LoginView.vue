<script setup lang="ts">
import LoginBrandPanel from '@/components/auth/LoginBrandPanel.vue'
import LoginForm from '@/components/auth/LoginForm.vue'
import { useLogin } from '@/composables/useLogin'

const { isSubmitting, errorMessage, submit } = useLogin()
</script>

<template>
  <main class="login-page">
    <div class="login-page__frame">
      <LoginBrandPanel />
      <LoginForm :loading="isSubmitting" :error-message="errorMessage" @submit="submit" />
    </div>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 28px; background: #edf3f8; }
.login-page__frame { width: min(1180px, 100%); min-height: 640px; display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(420px, .8fr); overflow: hidden; border: 1px solid rgb(8 54 92 / 9%); border-radius: 24px; background: #fff; box-shadow: 0 28px 80px rgb(7 48 82 / 15%); }
.login-page__frame > :last-child { padding: 48px; }

@media (max-width: 900px) {
  .login-page { padding: 18px; }
  .login-page__frame { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .login-page { padding: 0; background: #fff; }
  .login-page__frame { min-height: 100vh; border: 0; border-radius: 0; box-shadow: none; }
  .login-page__frame > :last-child { padding: 36px 24px; }
}
</style>
