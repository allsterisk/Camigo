import { computed, readonly, shallowRef } from 'vue'

import { getCourierTasks } from '@/api/delivery-order'
import type { CourierDeliveryOrder } from '@/types/delivery-order'
import { getDeliveryOrderTime, isToday } from '@/utils/delivery-order'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierDashboard() {
  const availableCount = shallowRef(0)
  const processingTasks = shallowRef<CourierDeliveryOrder[]>([])
  const deliveredTasks = shallowRef<CourierDeliveryOrder[]>([])
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')

  const completedToday = computed(() => (
    deliveredTasks.value.filter((order) => isToday(order.completedTime))
  ))
  const todayTaskCount = computed(() => processingTasks.value.length + completedToday.value.length)
  const efficiency = computed(() => (
    todayTaskCount.value === 0
      ? 0
      : Math.round((completedToday.value.length / todayTaskCount.value) * 100)
  ))
  const recentTasks = computed(() => (
    [...processingTasks.value, ...deliveredTasks.value]
      .sort((left, right) => (
        new Date(getDeliveryOrderTime(right)).getTime()
        - new Date(getDeliveryOrderTime(left)).getTime()
      ))
      .slice(0, 5)
  ))

  async function load() {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      const [waiting, processing, delivered] = await Promise.all([
        getCourierTasks({ page: 1, pageSize: 1, status: 'WAITING' }),
        getCourierTasks({ page: 1, pageSize: 200, status: 'PROCESSING' }),
        getCourierTasks({ page: 1, pageSize: 200, status: 'DELIVERED' }),
      ])
      availableCount.value = waiting.total
      processingTasks.value = processing.records
      deliveredTasks.value = delivered.records
    } catch (error) {
      availableCount.value = 0
      processingTasks.value = []
      deliveredTasks.value = []
      errorMessage.value = getRequestErrorMessage(error, '配送概览加载失败')
    } finally {
      loading.value = false
    }
  }

  return {
    availableCount: readonly(availableCount),
    processingTasks: readonly(processingTasks),
    completedToday,
    todayTaskCount,
    efficiency,
    recentTasks,
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    load,
  }
}
