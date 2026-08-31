import type {
  CampusLocationOption,
  CourierExecutionResult,
  CourierLocationState,
  CourierRoute,
} from '@/types/courier-execution'
import type { CourierStorageResult, LockerSlotOption } from '@/types/locker'
import type { ParcelException, ParcelExceptionType } from '@/types/parcel-exception'
import { get, post, put } from '@/utils/request'

export function getCampusLocationOptions(): Promise<CampusLocationOption[]> {
  return get<CampusLocationOption[]>('/couriers/locations')
}

export function getCourierLocationState(): Promise<CourierLocationState> {
  return get<CourierLocationState>('/couriers/me/location')
}

export function confirmCourierLocation(locationId: number): Promise<CourierLocationState> {
  return put<CourierLocationState, { locationId: number }>(
    '/couriers/me/location',
    { locationId },
  )
}

export function setCourierOffline(): Promise<void> {
  return put<void, undefined>('/couriers/me/offline')
}

export function getCourierRoute(): Promise<CourierRoute> {
  return get<CourierRoute>('/courier-execution/route')
}

export function confirmOrderPickup(orderId: number): Promise<CourierExecutionResult> {
  return post<CourierExecutionResult, undefined>(
    `/courier-execution/orders/${orderId}/pickup`,
    undefined,
  )
}

export function confirmOrderArrival(orderId: number): Promise<CourierExecutionResult> {
  return post<CourierExecutionResult, undefined>(
    `/courier-execution/orders/${orderId}/arrival`,
    undefined,
  )
}

export function getCompatibleLockerSlots(orderId: number): Promise<LockerSlotOption[]> {
  return get<LockerSlotOption[]>(`/courier-execution/orders/${orderId}/locker-slots`)
}

export function storeOrderInLocker(
  orderId: number,
  lockerSlotId: number,
): Promise<CourierStorageResult> {
  return post<CourierStorageResult, { lockerSlotId: number }>(
    `/courier-execution/orders/${orderId}/storage`,
    { lockerSlotId },
  )
}

export function reportParcelException(
  orderId: number,
  payload: { exceptionType: ParcelExceptionType; description: string },
): Promise<ParcelException> {
  return post<ParcelException, typeof payload>(`/courier-execution/orders/${orderId}/exceptions`, payload)
}
