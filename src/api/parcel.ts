import type { PageResult } from '@/types/api'
import type { Parcel, ParcelQuery } from '@/types/parcel'
import { get } from '@/utils/request'

export function getMyParcels(query: ParcelQuery): Promise<PageResult<Parcel>> {
  return get<PageResult<Parcel>>('/parcels/my', { params: query })
}

export function getParcelDetail(id: number): Promise<Parcel> {
  return get<Parcel>(`/parcels/${id}`)
}
