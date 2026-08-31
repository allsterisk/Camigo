import type { PageResult } from '@/types/api'
import type { Parcel, ParcelQuery } from '@/types/parcel'
import type { ParcelPickupAccess, PickupResult } from '@/types/locker'
import { get, post } from '@/utils/request'

export function getMyParcels(query: ParcelQuery): Promise<PageResult<Parcel>> {
  return get<PageResult<Parcel>>('/parcels/my', { params: query })
}

export function getParcelDetail(id: number): Promise<Parcel> {
  return get<Parcel>(`/parcels/${id}`)
}

export function getParcelPickupAccess(id: number): Promise<ParcelPickupAccess> {
  return get<ParcelPickupAccess>(`/parcels/${id}/pickup-access`)
}

export function pickupParcel(id: number, pickupCode: string): Promise<PickupResult> {
  return post<PickupResult, { parcelId: number; pickupCode: string }>('/lockers/pickup', {
    parcelId: id,
    pickupCode,
  })
}
