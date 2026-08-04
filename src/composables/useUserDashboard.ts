import { computed, readonly, shallowRef } from 'vue'

import { getMyParcels } from '@/api/parcel'
import type { Parcel } from '@/types/parcel'
import { getRequestErrorMessage } from '@/utils/request'

export function useUserDashboard() {
  const recentParcels = shallowRef<Parcel[]>([])
  const total = shallowRef(0)
  const awaitingPickup = shallowRef(0)
  const delivering = shallowRef(0)
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')

  const summaryItems = computed(() => [
    { label: '我的快递', value: total.value, hint: '当前账号全部快递', tone: 'blue' as const },
    { label: '待取件', value: awaitingPickup.value, hint: '请及时前往快递柜', tone: 'amber' as const },
    { label: '配送中', value: delivering.value, hint: '正在校园内配送', tone: 'green' as const },
  ])

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try {
      const [recentResult, storedResult, deliveringResult] = await Promise.all([
        getMyParcels({ page: 1, pageSize: 5 }),
        getMyParcels({ page: 1, pageSize: 1, status: 'STORED' }),
        getMyParcels({ page: 1, pageSize: 1, status: 'DELIVERING' }),
      ])
      recentParcels.value = recentResult.records
      total.value = recentResult.total
      awaitingPickup.value = storedResult.total
      delivering.value = deliveringResult.total
    } catch (error) {
      recentParcels.value = []
      total.value = 0
      awaitingPickup.value = 0
      delivering.value = 0
      errorMessage.value = getRequestErrorMessage(error, '首页数据加载失败')
    } finally {
      loading.value = false
    }
  }

  return {
    recentParcels: readonly(recentParcels),
    summaryItems,
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    load,
  }
}
