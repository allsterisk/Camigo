import { readonly, shallowRef } from 'vue'
import { getAdminParcels, startAdminParcelDelivery, updateAdminParcelStatus } from '@/api/admin'
import type { AdminParcelQuery } from '@/types/admin'
import type { Parcel } from '@/types/parcel'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminParcels() {
  const records = shallowRef<Parcel[]>([])
  const total = shallowRef(0)
  const loading = shallowRef(false)
  const actionId = shallowRef<number | null>(null)
  const errorMessage = shallowRef('')
  async function load(query: AdminParcelQuery) {
    loading.value = true; errorMessage.value = ''
    try { const result = await getAdminParcels(query); records.value = result.records; total.value = result.total }
    catch (error) { records.value = []; total.value = 0; errorMessage.value = getRequestErrorMessage(error, '快递列表加载失败') }
    finally { loading.value = false }
  }
  async function changeStatus(id: number, status: string) {
    if (actionId.value !== null) return false
    actionId.value = id
    try { await updateAdminParcelStatus(id, status); return true }
    catch (error) { errorMessage.value = getRequestErrorMessage(error, '快递状态更新失败'); return false }
    finally { actionId.value = null }
  }
  async function startDelivery(id: number) {
    if (actionId.value !== null) return false
    actionId.value = id
    errorMessage.value = ''
    try { await startAdminParcelDelivery(id); return true }
    catch (error) { errorMessage.value = getRequestErrorMessage(error, '新一轮智能派单启动失败'); return false }
    finally { actionId.value = null }
  }
  return { records: readonly(records), total: readonly(total), loading: readonly(loading),
    actionId: readonly(actionId), errorMessage: readonly(errorMessage), load, changeStatus, startDelivery }
}
