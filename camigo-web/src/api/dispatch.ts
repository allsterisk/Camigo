import type {
  DispatchAcceptance,
  DispatchAssignmentDetail,
  DispatchOffer,
  DispatchRejectReason,
} from '@/types/dispatch'
import { get, put } from '@/utils/request'

export async function getCurrentDispatchOffers(): Promise<DispatchOffer[]> {
  const result = await get<DispatchOffer[] | DispatchOffer | null>('/delivery-assignments/current')
  if (Array.isArray(result)) return result
  return result ? [result] : []
}

export function acceptDispatchOffer(assignmentId: number): Promise<DispatchAcceptance> {
  return put(`/delivery-assignments/${assignmentId}/accept`)
}

export function rejectDispatchOffer(
  assignmentId: number,
  payload: { reasonCode: DispatchRejectReason; reasonDetail?: string },
): Promise<void> {
  return put(`/delivery-assignments/${assignmentId}/reject`, payload)
}

export function getParcelDispatchDetails(parcelId: number): Promise<DispatchAssignmentDetail[]> {
  return get(`/delivery-assignments/parcels/${parcelId}`)
}
