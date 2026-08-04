<script setup lang="ts">
import { onMounted } from 'vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAdminDashboard } from '@/composables/useAdminDashboard'
const { summary, loading, errorMessage, load } = useAdminDashboard()
const cards = [
  { key: 'users', label: '平台用户', path: '/admin/users', unit: '人' },
  { key: 'parcels', label: '快递总量', path: '/admin/parcels', unit: '件' },
  { key: 'couriers', label: '配送员', path: '/admin/couriers', unit: '人' },
  { key: 'lockers', label: '快递柜', path: '/admin/lockers', unit: '台' },
] as const
onMounted(load)
</script>
<template>
  <div>
    <PageHeader eyebrow="OPERATIONS" title="运营概览" description="快速掌握平台核心资源；趋势图表将在 Step 6 完成。" refreshable :refreshing="loading" @refresh="load" />
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
    <section class="admin-dashboard__grid">
      <RouterLink v-for="card in cards" :key="card.key" :to="card.path" class="admin-dashboard__card">
        <span>{{ card.label }}</span><strong>{{ summary[card.key] }}</strong><small>{{ card.unit }} · 查看详情 →</small>
      </RouterLink>
    </section>
  </div>
</template>
<style scoped>
.admin-dashboard__grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; margin-top: 22px; }
.admin-dashboard__card { padding: 26px; border: 1px solid #e6deec; border-radius: 18px; color: inherit; background: #fff; text-decoration: none; box-shadow: 0 12px 30px rgb(83 48 104 / 6%); }
.admin-dashboard__card span,.admin-dashboard__card small { display: block; color: #7c6f83; }.admin-dashboard__card strong { display: block; margin: 18px 0 8px; color: #653983; font-size: 38px; }
@media (max-width: 900px) { .admin-dashboard__grid { grid-template-columns: repeat(2, 1fr); } }
</style>
