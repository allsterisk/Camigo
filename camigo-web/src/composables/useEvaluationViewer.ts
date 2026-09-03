import { readonly, shallowRef } from 'vue'

import { getEvaluationByOrder, getEvaluationsByParcel } from '@/api/evaluation'
import type { Evaluation } from '@/types/evaluation'
import { getRequestErrorMessage } from '@/utils/request'

/**
 * 评价查看弹窗的统一加载逻辑。
 * - parcel：按快递查询，可能返回多条（后端已按评价时间倒序）
 * - order：按配送订单查询，至多一条
 */
export type EvaluationLookupMode = 'parcel' | 'order'

export function useEvaluationViewer(mode: EvaluationLookupMode) {
  const visible = shallowRef(false)
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  const evaluations = shallowRef<Evaluation[]>([])
  const targetId = shallowRef<number | null>(null)

  async function open(id: number) {
    visible.value = true
    targetId.value = id
    evaluations.value = []
    errorMessage.value = ''
    loading.value = true
    try {
      if (mode === 'parcel') {
        evaluations.value = await getEvaluationsByParcel(id)
      } else {
        const evaluation = await getEvaluationByOrder(id)
        evaluations.value = evaluation ? [evaluation] : []
      }
    } catch (error) {
      errorMessage.value = getRequestErrorMessage(error, '评价加载失败')
    } finally {
      loading.value = false
    }
  }

  function close() {
    if (loading.value) return
    visible.value = false
    targetId.value = null
    evaluations.value = []
    errorMessage.value = ''
  }

  return {
    visible,
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    evaluations: readonly(evaluations),
    targetId: readonly(targetId),
    open,
    close,
  }
}
