import { readonly, shallowRef } from 'vue'

import { getCourierTasks } from '@/api/delivery-order'
import type {
  CourierDeliveryOrder,
  CourierDeliveryOrderQuery,
} from '@/types/delivery-order'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierTasks() {
  const tasks = shallowRef<CourierDeliveryOrder[]>([])
  const total = shallowRef(0)
  const page = shallowRef(1)
  const pageSize = shallowRef(10)
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  let loadVersion = 0

  async function load(query: CourierDeliveryOrderQuery) {
    const currentVersion = ++loadVersion
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getCourierTasks(query)
      if (currentVersion !== loadVersion) return
      tasks.value = result.records
      total.value = result.total
      page.value = result.page
      pageSize.value = result.pageSize
    } catch (error) {
      if (currentVersion !== loadVersion) return
      tasks.value = []
      total.value = 0
      errorMessage.value = getRequestErrorMessage(error, '配送任务加载失败')
    } finally {
      if (currentVersion === loadVersion) loading.value = false
    }
  }

  return {
    tasks: readonly(tasks),
    total: readonly(total),
    page: readonly(page),
    pageSize: readonly(pageSize),
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    load,
  }
}
