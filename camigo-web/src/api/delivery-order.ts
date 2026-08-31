import type { PageResult } from '@/types/api'
import type {
  CourierDeliveryOrder,
  CourierDeliveryOrderQuery,
  UserDeliveryOrder,
  UserDeliveryOrderQuery,
} from '@/types/delivery-order'
import { get } from '@/utils/request'

export function getMyDeliveryOrders(
  query: UserDeliveryOrderQuery,
): Promise<PageResult<UserDeliveryOrder>> {
  return get<PageResult<UserDeliveryOrder>>('/delivery-orders/my', { params: query })
}

export function getCourierTasks(
  query: CourierDeliveryOrderQuery,
): Promise<PageResult<CourierDeliveryOrder>> {
  return get<PageResult<CourierDeliveryOrder>>('/delivery-orders/tasks', { params: query })
}
