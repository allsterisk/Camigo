import { readonly, shallowRef } from 'vue'

import { getCompatibleLockerSlots, storeOrderInLocker } from '@/api/courier-execution'
import type { CourierStorageResult, LockerSlotOption } from '@/types/locker'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierStorage() {
  const visible = shallowRef(false)
  const orderId = shallowRef<number | null>(null)
  const slots = shallowRef<LockerSlotOption[]>([])
  const loading = shallowRef(false)
  const submitting = shallowRef(false)
  const errorMessage = shallowRef('')

  async function open(targetOrderId: number) {
    orderId.value = targetOrderId
    slots.value = []
    errorMessage.value = ''
    visible.value = true
    loading.value = true
    try {
      slots.value = await getCompatibleLockerSlots(targetOrderId)
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '兼容格口加载失败，请重试')
    } finally {
      loading.value = false
    }
  }

  function close() {
    if (submitting.value) return
    visible.value = false
    orderId.value = null
    slots.value = []
    errorMessage.value = ''
  }

  async function store(lockerSlotId: number): Promise<CourierStorageResult | null> {
    if (orderId.value === null || submitting.value) return null
    submitting.value = true
    errorMessage.value = ''
    try {
      const result = await storeOrderInLocker(orderId.value, lockerSlotId)
      visible.value = false
      return result
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '入柜失败，请刷新格口后重试')
      return null
    } finally {
      submitting.value = false
    }
  }

  return {
    visible,
    orderId: readonly(orderId),
    slots: readonly(slots),
    loading: readonly(loading),
    submitting: readonly(submitting),
    errorMessage: readonly(errorMessage),
    open,
    close,
    store,
  }
}
