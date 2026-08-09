import type {
  CourierEfficiency,
  DailyParcelStatistic,
  PopularPickupTime,
} from '@/types/statistics'
import { get } from '@/utils/request'

export function getDailyParcelStatistics(
  startDate: string,
  endDate: string,
): Promise<DailyParcelStatistic[]> {
  return get('/statistics/parcels/daily', { params: { startDate, endDate } })
}

export function getCourierEfficiency(limit = 8): Promise<CourierEfficiency[]> {
  return get('/statistics/couriers/efficiency', { params: { limit } })
}

export function getPopularPickupTimes(days: number, limit = 24): Promise<PopularPickupTime[]> {
  return get('/statistics/pickups/popular-hours', { params: { days, limit } })
}
