<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import DashboardSummary from '@/components/user/DashboardSummary.vue'
import ParcelTable from '@/components/user/ParcelTable.vue'
import { useUserDashboard } from '@/composables/useUserDashboard'

const router = useRouter()
const { recentParcels, summaryItems, loading, errorMessage, load } = useUserDashboard()

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="我的快递概览" description="查看包裹数量、待取件提醒与最新配送状态。" refreshable :refreshing="loading" @refresh="load" />
    <DashboardSummary :items="summaryItems" />
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
    <LoadingState v-else-if="loading && recentParcels.length === 0" />
    <section v-else class="dashboard-recent">
      <header class="dashboard-recent__header">
        <div><h2>最近快递</h2><p>优先展示最新登记的五件快递</p></div>
        <el-button link type="primary" @click="router.push('/user/parcels')">查看全部</el-button>
      </header>
      <EmptyState v-if="recentParcels.length === 0" title="还没有快递记录" description="管理员录入属于你的快递后，会在这里显示。" action-label="查看我的快递" @action="router.push('/user/parcels')" />
      <ParcelTable v-else :parcels="recentParcels" :loading="loading" @view="(id) => router.push(`/user/parcels/${id}`)" />
    </section>
  </div>
</template>

<style scoped>
.dashboard-recent { padding: 24px; border: 1px solid #e0eaf2; border-radius: 18px; background: #fff; box-shadow: 0 12px 32px rgb(15 59 92 / 5%); }
.dashboard-recent__header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.dashboard-recent__header h2 { margin: 0; color: #244c6c; font-size: 18px; }
.dashboard-recent__header p { margin: 6px 0 0; color: #8a9ba9; font-size: 12px; }
</style>
