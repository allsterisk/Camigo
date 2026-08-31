import { readonly, ref, shallowRef } from 'vue'

import {
  confirmOrderArrival,
  confirmOrderPickup,
  getCourierRoute,
} from '@/api/courier-execution'
import type { CourierExecutionResult, CourierRoute } from '@/types/courier-execution'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierRoute() {
  const route = ref<CourierRoute | null>(null)
  const loading = shallowRef(false)
  const actionOrderId = shallowRef<number | null>(null)
  const errorMessage = shallowRef('')

  async function load() {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      route.value = await getCourierRoute()
    } catch (error) {
      route.value = null
      errorMessage.value = getRequestErrorMessage(error, '配送路线加载失败')
    } finally {
      loading.value = false
    }
  }

  function confirmPickup(orderId: number): Promise<CourierExecutionResult | null> {
    return runAction(orderId, () => confirmOrderPickup(orderId), '确认取件失败')
  }

  function confirmArrival(orderId: number): Promise<CourierExecutionResult | null> {
    return runAction(orderId, () => confirmOrderArrival(orderId), '确认到达失败')
  }

  async function runAction(
    orderId: number,
    action: () => Promise<CourierExecutionResult>,
    fallback: string,
  ): Promise<CourierExecutionResult | null> {
    if (actionOrderId.value !== null) return null
    actionOrderId.value = orderId
    errorMessage.value = ''
    try {
      const result = await action()
      route.value = result.route
      return result
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, fallback)
      return null
    } finally {
      actionOrderId.value = null
    }
  }

  return {
    route: readonly(route),
    loading: readonly(loading),
    actionOrderId: readonly(actionOrderId),
    errorMessage: readonly(errorMessage),
    load,
    confirmPickup,
    confirmArrival,
  }
}
