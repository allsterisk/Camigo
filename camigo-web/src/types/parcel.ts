export const PARCEL_STATUSES = ['ARRIVED', 'DELIVERING', 'STORED', 'PICKED', 'COMPLETED'] as const

export type ParcelStatus = (typeof PARCEL_STATUSES)[number]

export interface Parcel {
  id: number
  trackingNo: string
  carrierCode: string
  recipientUserId: number
  recipientName: string | null
  senderName: string
  senderPhoneMasked: string | null
  destinationLocationId: number
  targetLockerId: number
  targetLockerName: string | null
  targetLockerAddress: string | null
  sizeClass: string
  weightKg: number | null
  currentStatus: ParcelStatus | string
  deliveryStartAvailable: boolean
  arriveTime: string
  createTime: string
  updateTime: string
}

export interface ParcelQuery {
  page: number
  pageSize: number
  status?: ParcelStatus | ''
  trackingNo?: string
}
