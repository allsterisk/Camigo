import { readonly, shallowRef } from 'vue'

import { createEvaluation } from '@/api/evaluation'
import type { Evaluation, EvaluationPayload } from '@/types/evaluation'
import { getRequestErrorMessage } from '@/utils/request'

export function useEvaluation() {
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  const result = shallowRef<Evaluation | null>(null)

  async function submit(payload: EvaluationPayload) {
    if (loading.value) return
    loading.value = true
    errorMessage.value = ''
    try {
      result.value = await createEvaluation(payload)
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
    submit,
    reset,
  }
}
