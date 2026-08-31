export interface DispatchOffer {
  assignmentId: number
  deliveryOrderId: number
  orderNo: string
  pickupLocationId: number
  pickupLocationName: string
  destinationLocationId: number
  destinationLocationName: string
  routeIncrementMeters: number
  totalScore: number
  offerTime: string
  offerExpireTime: string
}

export interface DispatchAcceptance {
  deliveryOrderId: number
  orderNo: string
  courierId: number
  status: string
  acceptedTime: string
}

export interface DispatchAssignmentDetail {
  assignmentId: number
  deliveryOrderId: number
  orderNo: string
  courierId: number
  courierNo: string
  courierName: string
  assignmentRound: number
  decision: string
  routeIncrementMeters: number | null
  activeOrderCount: number | null
  distanceScore: number
  workloadScore: number
  serviceScore: number
  totalScore: number
  algorithmVersion: string
  decisionReason: string | null
  offerTime: string | null
  offerExpireTime: string | null
  responseTime: string | null
  rejectReasonCode: string | null
  rejectReasonDetail: string | null
}

export type DispatchRejectReason = 'TOO_FAR' | 'TOO_BUSY' | 'VEHICLE_ISSUE' | 'OTHER'
