import type { PageResult } from '@/types/api'
import type { UserDeliveryOrder, UserDeliveryOrderQuery } from '@/types/delivery-order'
import { get } from '@/utils/request'

export function getMyDeliveryOrders(
  query: UserDeliveryOrderQuery,
): Promise<PageResult<UserDeliveryOrder>> {
  return get<PageResult<UserDeliveryOrder>>('/delivery-orders/my', { params: query })
}
