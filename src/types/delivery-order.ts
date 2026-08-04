export type DeliveryOrderStatus = 'WAITING' | 'PROCESSING' | 'DELIVERED'

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
