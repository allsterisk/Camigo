import { readonly, shallowRef } from 'vue'
import { reportParcelException } from '@/api/courier-execution'
import type { ParcelExceptionType } from '@/types/parcel-exception'
import { getRequestErrorMessage } from '@/utils/request'

export function useCourierExceptions() {
  const visible = shallowRef(false)
  const orderId = shallowRef<number | null>(null)
  const submitting = shallowRef(false)
  const errorMessage = shallowRef('')

  function open(targetOrderId: number) {
    orderId.value = targetOrderId
    errorMessage.value = ''
    visible.value = true
  }
  function close() {
    if (submitting.value) return
    visible.value = false
    orderId.value = null
    errorMessage.value = ''
  }
  async function submit(exceptionType: ParcelExceptionType, description: string) {
    if (orderId.value === null || submitting.value) return false
    submitting.value = true
    errorMessage.value = ''
    try {
      await reportParcelException(orderId.value, { exceptionType, description })
      visible.value = false
      orderId.value = null
      return true
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '异常上报失败，请确认任务状态后重试')
      return false
    } finally {
      submitting.value = false
    }
  }
  return { visible, orderId: readonly(orderId), submitting: readonly(submitting),
    errorMessage: readonly(errorMessage), open, close, submit }
}
