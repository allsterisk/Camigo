export type DeliveryOrderStatus = 'WAITING' | 'PROCESSING' | 'DELIVERED'

export interface CourierDeliveryOrder {
  id: number
  orderNo: string
  parcelId: number
  trackingNo: string
  carrierCode: string
  pickupLocationId: number
  pickupLocationName: string
  destinationLocationId: number
  destinationLocationName: string
  assignedCourierId: number | null
  sizeClass: 'S' | 'M' | 'L' | 'XL'
  weightKg: number | null
  status: DeliveryOrderStatus
  executionStatus: 'ACCEPTED' | 'PICKING_UP' | 'IN_TRANSIT' | 'ARRIVED' | string
  routeSequenceNo: number | null
  estimatedArrivalTime: string | null
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
