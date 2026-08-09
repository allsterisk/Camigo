import { computed, readonly, shallowRef } from 'vue'

import { getAdminCouriers, getAdminLockers, getAdminParcels, getAdminUsers } from '@/api/admin'
import {
  getCourierEfficiency,
  getDailyParcelStatistics,
  getPopularPickupTimes,
} from '@/api/statistics'
import type {
  CourierEfficiency,
  DailyParcelStatistic,
  PopularPickupTime,
} from '@/types/statistics'
import { getRequestErrorMessage } from '@/utils/request'

interface ResourceSummary {
  users: number
  parcels: number
  couriers: number
  lockers: number
}

export function useAdminDashboard() {
  const resources = shallowRef<ResourceSummary>({ users: 0, parcels: 0, couriers: 0, lockers: 0 })
  const parcelStatistics = shallowRef<DailyParcelStatistic[]>([])
  const courierEfficiency = shallowRef<CourierEfficiency[]>([])
  const pickupTimes = shallowRef<PopularPickupTime[]>([])
  const loading = shallowRef(false)
  const errorMessage = shallowRef('')
  const updatedAt = shallowRef<Date | null>(null)
  let loadVersion = 0

  const periodMetrics = computed(() => {
    const totals = parcelStatistics.value.reduce(
      (result, item) => ({
        parcels: result.parcels + item.parcelCount,
        picked: result.picked + item.pickedUpCount,
        exceptions: result.exceptions + item.exceptionCount,
      }),
      { parcels: 0, picked: 0, exceptions: 0 },
    )
    return {
      ...totals,
      pickupRate: totals.parcels > 0 ? Math.round((totals.picked / totals.parcels) * 1000) / 10 : 0,
      activeCouriers: courierEfficiency.value.filter((item) => item.totalOrders > 0).length,
    }
  })

  async function load(days: number) {
    const currentVersion = ++loadVersion
    const { startDate, endDate } = getDateRange(days)
    loading.value = true
    errorMessage.value = ''

    try {
      const [users, parcels, couriers, lockers, daily, ranking, peaks] = await Promise.all([
        getAdminUsers({ page: 1, pageSize: 1 }),
        getAdminParcels({ page: 1, pageSize: 1 }),
        getAdminCouriers({ page: 1, pageSize: 1 }),
        getAdminLockers({ page: 1, pageSize: 1 }),
        getDailyParcelStatistics(startDate, endDate),
        getCourierEfficiency(8),
        getPopularPickupTimes(days, 24),
      ])
      if (currentVersion !== loadVersion) return

      resources.value = {
        users: users.total,
        parcels: parcels.total,
        couriers: couriers.total,
        lockers: lockers.total,
      }
      parcelStatistics.value = daily
      courierEfficiency.value = ranking
      pickupTimes.value = peaks
      updatedAt.value = new Date()
    } catch (error) {
      if (currentVersion !== loadVersion) return
      errorMessage.value = getRequestErrorMessage(error, '运营数据加载失败，请稍后重试')
    } finally {
      if (currentVersion === loadVersion) loading.value = false
    }
  }

  return {
    resources: readonly(resources),
    parcelStatistics: readonly(parcelStatistics),
    courierEfficiency: readonly(courierEfficiency),
    pickupTimes: readonly(pickupTimes),
    periodMetrics,
    loading: readonly(loading),
    errorMessage: readonly(errorMessage),
    updatedAt: readonly(updatedAt),
    load,
  }
}

function getDateRange(days: number): { startDate: string; endDate: string } {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - days + 1)
  return { startDate: formatDate(start), endDate: formatDate(end) }
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
