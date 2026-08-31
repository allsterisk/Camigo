import type { CourierRoute } from '@/types/courier-execution'

export interface LockerSlotOption {
  id: number
  lockerId: number
  lockerName: string
  slotNo: string
  sizeClass: 'S' | 'M' | 'L' | 'XL'
}

export interface CourierStorageResult {
  storageRecordId: number
  orderId: number
  orderStatus: 'COMPLETED'
  parcelStatus: 'READY_PICKUP'
  lockerId: number
  lockerSlotId: number
  slotNo: string
  currentLocationId: number
  route: CourierRoute
}

export interface ParcelPickupAccess {
  parcelId: number
  storageRecordId: number
  lockerId: number
  lockerName: string
  lockerSlotId: number
  slotNo: string
  pickupCode: string | null
  storedTime: string
}

export interface PickupResult {
  id: number
  storageRecordId: number
  userId: number
  verificationType: 'PICKUP_CODE'
  result: 'SUCCESS'
  pickupTime: string
}
