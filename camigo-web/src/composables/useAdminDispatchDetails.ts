import { readonly, shallowRef } from 'vue'
import { getParcelDispatchDetails } from '@/api/dispatch'
import type { DispatchAssignmentDetail } from '@/types/dispatch'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminDispatchDetails() {
  const visible = shallowRef(false)
  const parcelId = shallowRef<number | null>(null)
  const records = shallowRef<DispatchAssignmentDetail[]>([])
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')

  async function open(id: number) {
    visible.value = true
    parcelId.value = id
    records.value = []
    errorMessage.value = ''
    loading.value = true
    try {
      records.value = await getParcelDispatchDetails(id)
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '派单详情加载失败')
    } finally {
      loading.value = false
    }
  }

  function close() {
    if (loading.value) return
    visible.value = false
    parcelId.value = null
    records.value = []
    errorMessage.value = ''
  }

  return {
    visible, parcelId: readonly(parcelId), records: readonly(records),
    loading: readonly(loading), errorMessage: readonly(errorMessage), open, close,
  }
}
