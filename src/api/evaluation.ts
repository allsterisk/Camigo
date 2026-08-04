import type { Evaluation, EvaluationPayload } from '@/types/evaluation'
import { post } from '@/utils/request'

export function createEvaluation(payload: EvaluationPayload): Promise<Evaluation> {
  return post<Evaluation, EvaluationPayload>('/evaluations', payload)
}
