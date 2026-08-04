import { readonly, shallowRef } from 'vue'

import { getMyParcels } from '@/api/parcel'
import type { Parcel, ParcelQuery } from '@/types/parcel'
import { getRequestErrorMessage } from '@/utils/request'

export function useUserParcels() {
  const parcels = shallowRef<Parcel[]>([])
  const total = shallowRef(0)
  const page = shallowRef(1)
  const pageSize = shallowRef(10)
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')

  async function load(query: ParcelQuery) {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getMyParcels(query)
      parcels.value = result.records
      total.value = result.total
      page.value = result.page
      pageSize.value = result.pageSize
    } catch (error) {
      parcels.value = []
      total.value = 0
      errorMessage.value = getRequestErrorMessage(error, '快递数据加载失败')
    } finally {
      loading.value = false
    }
  }

  return {
    parcels: readonly(parcels),
    total: readonly(total),
    page: readonly(page),
    pageSize: readonly(pageSize),
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    load,
  }
}
