import { readonly, shallowRef } from 'vue'
import { getAdminCouriers, getAdminLockers, getAdminParcels, getAdminUsers } from '@/api/admin'
import { getRequestErrorMessage } from '@/utils/request'

export function useAdminDashboard() {
  const summary = shallowRef({ users: 0, parcels: 0, couriers: 0, lockers: 0 })
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  async function load() {
    loading.value = true; errorMessage.value = ''
    try {
      const [users, parcels, couriers, lockers] = await Promise.all([
        getAdminUsers({ page: 1, pageSize: 1 }), getAdminParcels({ page: 1, pageSize: 1 }),
        getAdminCouriers({ page: 1, pageSize: 1 }), getAdminLockers({ page: 1, pageSize: 1 }),
      ])
      summary.value = { users: users.total, parcels: parcels.total, couriers: couriers.total, lockers: lockers.total }
    } catch (error) { errorMessage.value = getRequestErrorMessage(error, '运营概览加载失败') }
    finally { loading.value = false }
  }
  return { summary: readonly(summary), loading: readonly(loading), errorMessage: readonly(errorMessage), load }
}
