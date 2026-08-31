export interface CampusLocationOption {
  id: number
  locationCode: string
  locationName: string
}
export interface CourierLocationState {
  courierId: number
  currentLocationId: number | null
  currentLocationName: string | null
  locationUpdateTime: string | null
  availabilityStatus: 'OFFLINE' | 'AVAILABLE' | 'BUSY'
  activeOrderCount: number
  locationConfirmed: boolean
}

export interface CourierRouteStep {
  sequenceNo: number
  locationId: number
  locationName: string
  pickupOrderNos: string[]
  dropoffOrderNos: string[]
  estimatedArrivalTime: string | null
}

export interface CourierRoute {
  courierId: number
  currentLocationId: number
  currentLocationName: string
  versionNo: number | null
  totalDistanceMeters: number
  estimatedDurationSeconds: number
  algorithmVersion: string | null
  steps: CourierRouteStep[]
}

export interface CourierExecutionResult {
  orderId: number
  orderNo: string
  orderStatus: string
  parcelStatus: string
  currentLocationId: number
  currentLocationName: string
  route: CourierRoute
}
