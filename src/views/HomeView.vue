<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const cards = computed(() => {
  const items = []
  if (authStore.hasRole('USER')) {
    items.push({ title: '我的快递', text: '查询包裹状态并确认取件完成', to: '/parcels', action: '查看快递' })
  }
  if (authStore.hasRole('COURIER')) {
    items.push({ title: '配送任务', text: '查看待接任务，接单并更新配送进度', to: '/delivery-tasks', action: '处理任务' })
  }
  if (authStore.hasRole('ADMIN')) {
    items.push({ title: '管理员账号', text: '管理端页面将在后续模块中接入', to: null, action: '即将开放' })
  }
  return items
})
</script>

<template>
  <section>
    <div class="page-heading welcome-heading">
      <div>
        <p class="eyebrow">CAMIGO WORKSPACE</p>
        <h1>你好，{{ authStore.currentUser?.realName || authStore.currentUser?.username }}</h1>
        <p class="muted">今天也让每一件快递准确、及时地抵达。</p>
      </div>
      <el-tag v-for="role in authStore.roles" :key="role" effect="plain">{{ role }}</el-tag>
    </div>

    <div class="feature-grid">
      <el-card v-for="card in cards" :key="card.title" class="feature-card" shadow="hover">
        <span class="feature-index">{{ String(cards.indexOf(card) + 1).padStart(2, '0') }}</span>
        <h2>{{ card.title }}</h2>
        <p>{{ card.text }}</p>
        <RouterLink v-if="card.to" :to="card.to">{{ card.action }} →</RouterLink>
        <span v-else class="muted">{{ card.action }}</span>
      </el-card>
    </div>
  </section>
</template>
