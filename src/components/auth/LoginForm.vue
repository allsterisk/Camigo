<script setup lang="ts">
import { reactive, useTemplateRef } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import type { LoginCredentials } from '@/types/auth'

defineProps<{
  loading: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  submit: [credentials: LoginCredentials]
}>()

const formRef = useTemplateRef<FormInstance>('formRef')
const form = reactive<LoginCredentials>({ account: '', password: '' })
const rules: FormRules<LoginCredentials> = {
  account: [
    { required: true, message: '请输入用户名、手机号或学号', trigger: 'blur' },
    { max: 50, message: '账号不能超过 50 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 位', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid) emit('submit', { ...form })
}
</script>

<template>
  <section class="login-form-panel">
    <header class="login-form-panel__header">
      <span class="login-form-panel__eyebrow">WELCOME BACK</span>
      <h2 class="login-form-panel__title">登录 Camigo</h2>
      <p class="login-form-panel__subtitle">使用用户名、手机号或学号登录</p>
    </header>

    <el-alert
      v-if="errorMessage"
      class="login-form-panel__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <el-form
      ref="formRef"
      class="login-form"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="账号" prop="account">
        <el-input
          v-model.trim="form.account"
          name="username"
          autocomplete="username"
          maxlength="50"
          placeholder="请输入用户名、手机号或学号"
          clearable
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="current-password"
          maxlength="64"
          placeholder="请输入密码"
          show-password
          @keyup.enter="handleSubmit"
        />
      </el-form-item>

      <el-button class="login-form__submit" type="primary" native-type="submit" :loading="loading">
        {{ loading ? '正在验证…' : '安全登录' }}
      </el-button>
    </el-form>

    <p class="login-form-panel__notice">账号由学校统一开通。如无法登录，请联系系统管理员。</p>
  </section>
</template>

<style scoped>
.login-form-panel { width: min(420px, 100%); margin: auto; }
.login-form-panel__header { margin-bottom: 32px; }
.login-form-panel__eyebrow { color: #176aa6; font-size: 11px; font-weight: 800; letter-spacing: .2em; }
.login-form-panel__title { margin: 12px 0 8px; color: #0b2f53; font-size: 34px; letter-spacing: -.03em; }
.login-form-panel__subtitle { margin: 0; color: #778a9d; }
.login-form-panel__error { margin-bottom: 22px; }
.login-form :deep(.el-form-item__label) { color: #274c6c; font-weight: 600; }
.login-form :deep(.el-input__wrapper) { min-height: 48px; border-radius: 10px; box-shadow: 0 0 0 1px #d8e3ed inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #1674ad inset, 0 0 0 4px rgb(22 116 173 / 9%); }
.login-form__submit { width: 100%; height: 48px; margin-top: 8px; border-radius: 10px; font-weight: 700; }
.login-form-panel__notice { margin: 26px 0 0; color: #91a0ae; font-size: 12px; line-height: 1.7; text-align: center; }
</style>
