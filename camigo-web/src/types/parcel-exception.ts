export type ParcelExceptionType = 'PACKAGE_DAMAGED' | 'RECIPIENT_INFO_ERROR' | 'LOCKER_FAILURE' | 'USER_UNREACHABLE' | 'OTHER'
export type ParcelExceptionStatus = 'PENDING' | 'RESOLVED' | 'RETURNED'
export type ParcelExceptionResolution = 'RESUME_DELIVERY' | 'RETURN_TO_SORTING' | 'CONFIRM_RETURNED'

export interface ParcelException {
  id: number
  parcelId: number
  trackingNo: string
  deliveryOrderId: number
  orderNo: string
  courierId: number
  courierName: string
  exceptionType: ParcelExceptionType
  description: string
  requiresAdmin: boolean
  status: ParcelExceptionStatus
  reportedTime: string
  handledByUserId: number | null
  handlerName: string | null
  handledTime: string | null
  resolutionType: ParcelExceptionResolution | null
  resolutionNote: string | null
}

export interface ParcelExceptionQuery {
  page: number
  pageSize: number
  status?: ParcelExceptionStatus | ''
  keyword?: string
}
