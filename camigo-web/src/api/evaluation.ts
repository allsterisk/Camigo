import type { Evaluation, EvaluationPayload } from '@/types/evaluation'
import { get, post } from '@/utils/request'

export function createEvaluation(payload: EvaluationPayload): Promise<Evaluation> {
  return post<Evaluation, EvaluationPayload>('/evaluations', payload)
}

/** 按快递查询其关联配送订单的全部评价（后端按评价时间倒序，最新一条在前）。 */
export function getEvaluationsByParcel(parcelId: number): Promise<Evaluation[]> {
  return get<Evaluation[]>(`/evaluations/parcel/${parcelId}`)
}

/** 按配送订单查询单条评价；未评价时返回 null。 */
export function getEvaluationByOrder(orderId: number): Promise<Evaluation | null> {
  return get<Evaluation | null>(`/evaluations/order/${orderId}`)
}
