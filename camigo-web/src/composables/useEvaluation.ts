import { readonly, shallowRef } from 'vue'

import { getMyDeliveryOrders } from '@/api/delivery-order'
import { createEvaluation } from '@/api/evaluation'
import type { UserDeliveryOrder } from '@/types/delivery-order'
import type { Evaluation, EvaluationPayload } from '@/types/evaluation'
import { getRequestErrorMessage } from '@/utils/request'

export function useEvaluation() {
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  const result = shallowRef<Evaluation | null>(null)
  const orders = shallowRef<UserDeliveryOrder[]>([])
  const ordersLoading = shallowRef(false)
  const ordersError = shallowRef('')

  async function loadOrders() {
    if (ordersLoading.value) return
    ordersLoading.value = true
    ordersError.value = ''
    try {
      const page = await getMyDeliveryOrders({
        page: 1,
        pageSize: 100,
        status: 'DELIVERED',
        evaluated: false,
      })
      orders.value = page.records
    } catch (error) {
      orders.value = []
      ordersError.value = getRequestErrorMessage(error, '待评价订单加载失败')
    } finally {
      ordersLoading.value = false
    }
  }

  async function submit(payload: EvaluationPayload) {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      result.value = await createEvaluation(payload)
      orders.value = orders.value.filter(
        (order) => order.deliveryOrderId !== payload.deliveryOrderId,
      )
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '评价提交失败')
    } finally {
      loading.value = false
    }
  }

  function reset() {
    result.value = null
    errorMessage.value = ''
  }

  return {
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    result: readonly(result),
    orders: readonly(orders),
    ordersLoading: readonly(ordersLoading),
    ordersError: readonly(ordersError),
    loadOrders,
    submit,
    reset,
  }
}
