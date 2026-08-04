import { readonly, shallowRef } from 'vue'
import { getAdminCouriers } from '@/api/admin'
import type { AdminCourier, AdminCourierQuery } from '@/types/admin'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminCouriers() {
  const records = shallowRef<AdminCourier[]>([]); const total = shallowRef(0)
  const loading = shallowRef(false); const errorMessage = shallowRef('')
  async function load(query: AdminCourierQuery) {
    loading.value = true; errorMessage.value = ''
    try { const result = await getAdminCouriers(query); records.value = result.records; total.value = result.total }
    catch (error) { records.value = []; total.value = 0; errorMessage.value = getRequestErrorMessage(error, '配送员列表加载失败') }
    finally { loading.value = false }
  }
  return { records: readonly(records), total: readonly(total), loading: readonly(loading), errorMessage: readonly(errorMessage), load }
}
