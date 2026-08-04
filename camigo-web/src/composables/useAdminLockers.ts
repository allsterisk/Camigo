import { readonly, shallowRef } from 'vue'
import { getAdminLockers } from '@/api/admin'
import type { AdminLocker, AdminLockerQuery } from '@/types/admin'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminLockers() {
  const records = shallowRef<AdminLocker[]>([]); const total = shallowRef(0)
  const loading = shallowRef(false); const errorMessage = shallowRef('')
  async function load(query: AdminLockerQuery) {
    loading.value = true; errorMessage.value = ''
    try { const result = await getAdminLockers(query); records.value = result.records; total.value = result.total }
    catch (error) { records.value = []; total.value = 0; errorMessage.value = getRequestErrorMessage(error, '快递柜列表加载失败') }
    finally { loading.value = false }
  }
  return { records: readonly(records), total: readonly(total), loading: readonly(loading), errorMessage: readonly(errorMessage), load }
}
