<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="phase-shell">
    <section class="phase-card">
      <span class="phase-card__eyebrow">404</span>
      <h1>页面不存在</h1>
      <p>当前地址没有对应的 Camigo 页面。</p>
      <el-button type="primary" @click="router.push('/')">返回入口</el-button>
    </section>
  </main>
</template>
