import type { ParcelQuery } from '@/types/parcel'

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'CANCELLED'
export type AdminRole = 'USER' | 'COURIER' | 'ADMIN'

export interface AdminUser {
  id: number
  username: string
  realName: string
  phone: string
  studentNo: string | null
  status: UserStatus
  roleCodes: string
  roles: AdminRole[]
  lastLoginTime: string | null
  createTime: string
  updateTime: string
}

export interface AdminUserQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: UserStatus | ''
  role?: AdminRole | ''
}

export interface AdminCourier {
  id: number
  userId: number
  courierNo: string
  realName: string
  phone: string
  employmentStatus: string
  availabilityStatus: string
  maxActiveOrders: number
  ratingAvg: number | null
  totalOrders: number
  completedOrders: number
  failedOrders: number
  avgDeliverySeconds: number | null
}

export interface AdminCourierQuery {
  page: number
  pageSize: number
  keyword?: string
  employmentStatus?: string
}

export interface AdminLocker {
  id: number
  lockerCode: string
  lockerName: string
  locationId: number
  locationName: string | null
  operationStatus: string
  totalSlots: number
  availableSlots: number
  reservedSlots: number
  occupiedSlots: number
  unavailableSlots: number
  usageRate: number
  lastHeartbeatTime: string | null
}

export interface AdminLockerQuery {
  page: number
  pageSize: number
  keyword?: string
  operationStatus?: string
}

export type AdminParcelQuery = ParcelQuery
