import { readonly, shallowRef } from 'vue'

import {
  acceptDeliveryOrder,
  completeDeliveryOrder,
  getCourierTasks,
} from '@/api/delivery-order'
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
  const actionOrderId = shallowRef<number | null>(null)
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

  async function accept(orderId: number): Promise<boolean> {
    return runAction(orderId, () => acceptDeliveryOrder(orderId), '接单失败')
  }

  async function complete(orderId: number): Promise<boolean> {
    return runAction(orderId, () => completeDeliveryOrder(orderId), '更新配送状态失败')
  }

  async function runAction(
    orderId: number,
    action: () => Promise<CourierDeliveryOrder>,
    fallbackMessage: string,
  ): Promise<boolean> {
    if (actionOrderId.value !== null) return false
    actionOrderId.value = orderId
    errorMessage.value = ''
    try {
      await action()
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, fallbackMessage)
      return false
    } finally {
      actionOrderId.value = null
    }
  }

  return {
    tasks: readonly(tasks),
    total: readonly(total),
    page: readonly(page),
    pageSize: readonly(pageSize),
    loading: readonly(loading),
    actionOrderId: readonly(actionOrderId),
    errorMessage: readonly(errorMessage),
    load,
    accept,
    complete,
  }
}
