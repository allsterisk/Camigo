import { readonly, shallowRef } from 'vue'
import { getAdminExceptions, resolveAdminException } from '@/api/admin'
import type { ParcelException, ParcelExceptionQuery, ParcelExceptionResolution } from '@/types/parcel-exception'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminExceptions() {
  const records = shallowRef<ParcelException[]>([])
  const total = shallowRef(0)
  const loading = shallowRef(false)
  const actionId = shallowRef<number | null>(null)
  const errorMessage = shallowRef('')
  async function load(query: ParcelExceptionQuery) {
    loading.value = true; errorMessage.value = ''
    try { const result = await getAdminExceptions(query); records.value = result.records; total.value = result.total }
    catch (error) { records.value = []; total.value = 0; errorMessage.value = getRequestErrorMessage(error, '异常记录加载失败') }
    finally { loading.value = false }
  }
  async function resolve(id: number, resolutionType: ParcelExceptionResolution, resolutionNote: string) {
    if (actionId.value !== null) return false
    actionId.value = id; errorMessage.value = ''
    try { await resolveAdminException(id, { resolutionType, resolutionNote }); return true }
    catch (error) { errorMessage.value = getRequestErrorMessage(error, '异常处理失败，请刷新状态后重试'); return false }
    finally { actionId.value = null }
  }
  return { records: readonly(records), total: readonly(total), loading: readonly(loading),
    actionId: readonly(actionId), errorMessage: readonly(errorMessage), load, resolve }
}
