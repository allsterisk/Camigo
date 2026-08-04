export interface EvaluationPayload {
  deliveryOrderId: number
  deliveryScore: number
  serviceScore: number
  content: string
}

export interface Evaluation {
  id: number
  deliveryOrderId: number
  userId: number
  courierId: number
  deliveryScore: number
  serviceScore: number
  content: string | null
  status: string
  evaluatedTime: string
}
