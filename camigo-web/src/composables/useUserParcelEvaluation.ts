import { readonly, shallowRef, watch } from 'vue'

import { getEvaluationsByParcel } from '@/api/evaluation'
import type { Evaluation } from '@/types/evaluation'
import type { Parcel } from '@/types/parcel'
import { getRequestErrorMessage } from '@/utils/request'

/**
 * 用户快递详情页加载「我的评价」。
 * 后端按评价时间倒序返回该快递关联配送订单的全部评价，这里只取最新一条。
 */
export function useUserParcelEvaluation(parcelGetter: () => Parcel | null) {
  const latest = shallowRef<Evaluation | null>(null)
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  let requestSequence = 0

  watch(
    parcelGetter,
    async (parcel) => {
      const sequence = ++requestSequence
      latest.value = null
      errorMessage.value = ''
      if (!parcel) return

      loading.value = true
      try {
        const evaluations = await getEvaluationsByParcel(parcel.id)
        if (sequence === requestSequence) latest.value = evaluations[0] ?? null
      } catch (error) {
        if (sequence === requestSequence) {
          errorMessage.value = getRequestErrorMessage(error, '评价加载失败')
        }
      } finally {
        if (sequence === requestSequence) loading.value = false
      }
    },
    { immediate: true },
  )

  return {
    latest: readonly(latest),
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
  }
}
