export interface DailyParcelStatistic {
  statDate: string
  carrierCode: string
  parcelCount: number
  pickedUpCount: number
  exceptionCount: number
}

export interface CourierEfficiency {
  courierId: number
  courierNo: string
  realName: string
  totalOrders: number
  completedOrders: number
  failedOrders: number
  avgDeliverySeconds: number | null
  avgRating: number | null
}

export interface PopularPickupTime {
  hourOfDay: number
  pickupCount: number
}
