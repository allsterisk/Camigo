import type { PageResult } from '@/types/api'
import type {
  CourierDeliveryOrder,
  CourierDeliveryOrderQuery,
  DeliveryOrderStatusPayload,
  UserDeliveryOrder,
  UserDeliveryOrderQuery,
} from '@/types/delivery-order'
import { get, put } from '@/utils/request'

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

export function acceptDeliveryOrder(id: number): Promise<CourierDeliveryOrder> {
  return put<CourierDeliveryOrder, undefined>(`/delivery-orders/${id}/accept`)
}

export function completeDeliveryOrder(id: number): Promise<CourierDeliveryOrder> {
  const payload: DeliveryOrderStatusPayload = { status: 'DELIVERED' }
  return put<CourierDeliveryOrder, DeliveryOrderStatusPayload>(
    `/delivery-orders/${id}/status`,
    payload,
  )
}
