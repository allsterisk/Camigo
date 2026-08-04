import type { PageResult } from '@/types/api'
import type {
  AdminCourier, AdminCourierQuery, AdminLocker, AdminLockerQuery,
  AdminParcelQuery, AdminUser, AdminUserQuery, UserStatus,
} from '@/types/admin'
import type { Parcel } from '@/types/parcel'
import { del, get, put } from '@/utils/request'

export function getAdminUsers(query: AdminUserQuery): Promise<PageResult<AdminUser>> {
  return get('/users', { params: query })
}

export function updateAdminUserStatus(id: number, status: UserStatus): Promise<AdminUser> {
  return put(`/users/${id}`, { status })
}

export function deleteAdminUser(id: number): Promise<void> {
  return del(`/users/${id}`)
}

export function getAdminParcels(query: AdminParcelQuery): Promise<PageResult<Parcel>> {
  return get('/parcels', { params: query })
}

export function updateAdminParcelStatus(id: number, status: string): Promise<Parcel> {
  return put(`/parcels/${id}/status`, { status, remark: '管理员后台更新' })
}

export function getAdminCouriers(query: AdminCourierQuery): Promise<PageResult<AdminCourier>> {
  return get('/couriers', { params: query })
}

export function getAdminLockers(query: AdminLockerQuery): Promise<PageResult<AdminLocker>> {
  return get('/lockers', { params: query })
}
