<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import CourierSummary from '@/components/courier/CourierSummary.vue'
import CourierTaskTable from '@/components/courier/CourierTaskTable.vue'
import { useCourierDashboard } from '@/composables/useCourierDashboard'

const router = useRouter()
const {
  availableCount,
  processingTasks,
  completedToday,
  todayTaskCount,
  efficiency,
  recentTasks,
  loading,
  errorMessage,
  load,
} = useCourierDashboard()

const summaryItems = computed(() => [
  {
    label: '今日任务',
    value: todayTaskCount.value,
    hint: `当前进行中 ${processingTasks.value.length} 单`,
    tone: 'teal' as const,
  },
  {
    label: '今日完成',
    value: completedToday.value.length,
    hint: `另有 ${availableCount.value} 单可接`,
    tone: 'blue' as const,
  },
  {
    label: '配送效率',
    value: `${efficiency.value}%`,
    hint: '今日已完成任务占比',
    tone: 'green' as const,
  },
])

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="COURIER CENTER"
      title="配送工作概览"
      description="集中查看今日任务、完成进度与最近配送动态。"
      refreshable
      :refreshing="loading"
      @refresh="load"
    />
    <CourierSummary :items="summaryItems" />
    <el-alert
      v-if="errorMessage"
      class="courier-dashboard__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <LoadingState v-if="loading && recentTasks.length === 0" />
    <section v-else class="courier-dashboard__recent">
      <header class="courier-dashboard__header">
        <div>
          <h2>最近任务</h2>
          <p>展示正在配送和最近完成的任务。</p>
        </div>
        <el-button link type="primary" @click="router.push('/courier/tasks')">
          进入任务中心
        </el-button>
      </header>
      <EmptyState
        v-if="recentTasks.length === 0"
        title="暂无配送任务"
        description="出现可接任务后，可以前往任务中心接单。"
        action-label="查看可接任务"
        @action="router.push('/courier/tasks')"
      />
      <CourierTaskTable v-else :tasks="recentTasks" />
    </section>
  </div>
</template>

<style scoped>
.courier-dashboard__error { margin-bottom: 18px; }
.courier-dashboard__recent { padding: 24px; border: 1px solid #dfeae9; border-radius: 18px; background: #fff; box-shadow: 0 12px 32px rgb(12 71 68 / 5%); }
.courier-dashboard__header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.courier-dashboard__header h2 { margin: 0; color: #224f4c; font-size: 18px; }
.courier-dashboard__header p { margin: 6px 0 0; color: #8a9d9b; font-size: 12px; }
</style>
