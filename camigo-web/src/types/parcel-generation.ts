export type ParcelSizeClass = 'S' | 'M' | 'L' | 'XL'

export interface AdminStudentOption {
  id: number
  realName: string
  studentNo: string
  phone: string
}

export interface CampusLocationOption {
  id: number
  locationCode: string
  locationName: string
}

export interface ParcelGenerationOptions {
  students: AdminStudentOption[]
  pickupLocations: CampusLocationOption[]
  destinationLocations: CampusLocationOption[]
  sizeClasses: ParcelSizeClass[]
  maxBatchSize: number
}

export interface ParcelGenerationRequest {
  recipientUserId: number
  pickupLocationId: number
  destinationLocationId: number
  sizeClass: ParcelSizeClass
  weightKg: number
  arriveTime?: string
  count: number
}

export interface GeneratedParcel {
  parcelId: number
  trackingNo: string
  carrierCode: string
  deliveryOrderId: number
  orderNo: string
  parcelStatus: string
  orderStatus: string
  dispatchRound: number
  targetLockerId: number
}
