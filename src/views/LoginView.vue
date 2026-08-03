<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const form = reactive({ account: '', password: '' })
const rules = {
  account: [{ required: true, message: '请输入用户名、手机号或学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await authStore.login(form.account.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch {
    // Axios 响应拦截器负责展示后端错误。
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="brand brand-light">
        <span class="brand-mark">C</span>
        <span><strong>Camigo</strong><small>校速达</small></span>
      </div>
      <h1>校园快递，准时抵达</h1>
      <p>一个入口连接收件用户、校园配送员与智能快递柜。</p>
    </section>
    <section class="login-panel">
      <el-card class="login-card" shadow="never">
        <h2>欢迎回来</h2>
        <p class="muted">登录后继续处理你的快递任务</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="账号" prop="account">
            <el-input v-model="form.account" size="large" autocomplete="username" placeholder="用户名 / 手机号 / 学号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password autocomplete="current-password" placeholder="请输入密码" />
          </el-form-item>
          <el-button class="login-button" type="primary" size="large" :loading="authStore.loading" @click="submit">
            登录
          </el-button>
        </el-form>
      </el-card>
    </section>
  </main>
</template>
