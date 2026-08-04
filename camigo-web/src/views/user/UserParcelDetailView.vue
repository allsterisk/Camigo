<script setup lang="ts">
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ParcelDetailPanel from '@/components/user/ParcelDetailPanel.vue'
import { useParcelDetail } from '@/composables/useParcelDetail'

const router = useRouter()
const { parcel, loading, errorMessage } = useParcelDetail()
</script>

<template>
  <div>
    <PageHeader eyebrow="PARCEL DETAIL" title="快递详情" description="查看当前状态、包裹信息和目标取件位置。" />
    <el-button class="parcel-detail-view__back" link type="primary" @click="router.push('/user/parcels')">← 返回我的快递</el-button>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="errorMessage || !parcel" title="无法查看该快递" :description="errorMessage || '快递信息不存在'" action-label="返回列表" @action="router.push('/user/parcels')" />
    <ParcelDetailPanel v-else :parcel="parcel" />
  </div>
</template>

<style scoped>
.parcel-detail-view__back { margin: -14px 0 20px; }
</style>
