<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ParcelDetailPanel from '@/components/user/ParcelDetailPanel.vue'
import PickupCodePanel from '@/components/user/PickupCodePanel.vue'
import { useParcelDetail } from '@/composables/useParcelDetail'
import { useParcelPickup } from '@/composables/useParcelPickup'

const router = useRouter()
const { parcel, loading, errorMessage, reload } = useParcelDetail()
const {
  access: pickupAccess,
  loading: pickupLoading,
  submitting: pickupSubmitting,
  errorMessage: pickupError,
  pickupResult,
  pickup,
} = useParcelPickup(parcel)

async function handlePickup(code: string) {
  if (!await pickup(code)) return
  ElMessage.success('取件成功，取件码已失效并释放格口')
  await reload()
}
</script>

<template>
  <div>
    <PageHeader title="快递详情" description="查看当前状态、包裹信息和目标取件位置。" />
    <el-button class="parcel-detail-view__back" link type="primary" @click="router.push('/user/parcels')">← 返回我的快递</el-button>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="errorMessage || !parcel" title="无法查看该快递" :description="errorMessage || '快递信息不存在'" action-label="返回列表" @action="router.push('/user/parcels')" />
    <template v-else>
      <ParcelDetailPanel :parcel="parcel" />
      <PickupCodePanel
        v-if="parcel.currentStatus === 'STORED'"
        class="parcel-detail-view__pickup"
        :access="pickupAccess"
        :loading="pickupLoading"
        :submitting="pickupSubmitting"
        :error-message="pickupError"
        @pickup="handlePickup"
      />
      <el-result v-else-if="pickupResult" class="parcel-detail-view__pickup-result" icon="success" title="取件成功" sub-title="取件码已立即失效，快递柜格口已经释放。">
        <template #extra><span>完成时间：{{ new Date(pickupResult.pickupTime).toLocaleString('zh-CN') }}</span></template>
      </el-result>
    </template>
  </div>
</template>

<style scoped>
.parcel-detail-view__back { margin: -14px 0 20px; }
.parcel-detail-view__pickup { margin-top: 20px; }
.parcel-detail-view__pickup-result { margin-top: 20px; border: 1px solid #d8e9e5; border-radius: 16px; background: #fff; }
</style>
