import { readonly, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'

import { getParcelDetail } from '@/api/parcel'
import type { Parcel } from '@/types/parcel'
import { getRequestErrorMessage } from '@/utils/request'

export function useParcelDetail() {
  const route = useRoute()
  const parcel = shallowRef<Parcel | null>(null)
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  let requestSequence = 0

  watch(
    () => route.params.id,
    async (rawId) => {
      const id = Number(rawId)
      const sequence = ++requestSequence
      parcel.value = null
      errorMessage.value = ''

      if (!Number.isInteger(id) || id <= 0) {
        errorMessage.value = '快递编号不正确'
        return
      }

      loading.value = true
      try {
        const result = await getParcelDetail(id)
        if (sequence === requestSequence) parcel.value = result
      } catch (error) {
        if (sequence === requestSequence) {
          errorMessage.value = getRequestErrorMessage(error, '快递详情加载失败')
        }
      } finally {
        if (sequence === requestSequence) loading.value = false
      }
    },
    { immediate: true },
  )

  return { parcel: readonly(parcel), loading: readonly(loading), errorMessage: readonly(errorMessage) }
}
