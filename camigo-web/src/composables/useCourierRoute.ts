import { readonly, ref, shallowRef } from 'vue'

import {
  confirmOrderArrival,
  confirmOrderPickup,
  confirmOrderPickupBatch,
  getCourierRoute,
} from '@/api/courier-execution'
import type {
  CourierBatchPickupResult,
  CourierExecutionResult,
  CourierRoute,
} from '@/types/courier-execution'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierRoute() {
  const route = ref<CourierRoute | null>(null)
  const loading = shallowRef(false)
  const actionOrderId = shallowRef<number | null>(null)
  const actionBusy = shallowRef(false)
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

  async function confirmPickupBatch(orderIds: number[]): Promise<CourierBatchPickupResult | null> {
    if (actionBusy.value || !orderIds.length) return null
    actionBusy.value = true
    actionOrderId.value = orderIds[0] ?? null
    errorMessage.value = ''
    try {
      const result = await confirmOrderPickupBatch(orderIds)
      route.value = result.route
      return result
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '批量取件失败')
      return null
    } finally {
      actionOrderId.value = null
      actionBusy.value = false
    }
  }

  function confirmArrival(orderId: number): Promise<CourierExecutionResult | null> {
    return runAction(orderId, () => confirmOrderArrival(orderId), '确认到达失败')
  }

  async function runAction(
    orderId: number,
    action: () => Promise<CourierExecutionResult>,
    fallback: string,
  ): Promise<CourierExecutionResult | null> {
    if (actionBusy.value) return null
    actionBusy.value = true
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
      actionBusy.value = false
    }
  }

  return {
    route: readonly(route),
    loading: readonly(loading),
    actionOrderId: readonly(actionOrderId),
    actionBusy: readonly(actionBusy),
    errorMessage: readonly(errorMessage),
    load,
    confirmPickup,
    confirmPickupBatch,
    confirmArrival,
  }
}
