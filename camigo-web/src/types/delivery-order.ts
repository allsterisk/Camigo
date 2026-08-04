export type DeliveryOrderStatus = 'WAITING' | 'PROCESSING' | 'DELIVERED'

export type DeliveryOrderPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export interface CourierDeliveryOrder {
  id: number
  orderNo: string
  parcelId: number
  destinationLocationId: number
  assignedCourierId: number | null
  priority: DeliveryOrderPriority | string
  status: DeliveryOrderStatus
  predictedMinutes: number | null
  acceptedTime: string | null
  completedTime: string | null
  createTime: string
  updateTime: string
}

export interface CourierDeliveryOrderQuery {
  page: number
  pageSize: number
  status?: DeliveryOrderStatus
}

export interface DeliveryOrderStatusPayload {
  status: 'DELIVERED'
}

export interface UserDeliveryOrder {
  deliveryOrderId: number
  orderNo: string
  parcelId: number
  trackingNo: string
  carrierCode: string
  courierId: number | null
  courierName: string | null
  status: DeliveryOrderStatus
  completedTime: string | null
  evaluated: boolean
}

export interface UserDeliveryOrderQuery {
  page: number
  pageSize: number
  status?: DeliveryOrderStatus
  evaluated?: boolean
}
