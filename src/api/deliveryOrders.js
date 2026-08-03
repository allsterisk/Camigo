import request from '../utils/request'

export function createDeliveryOrder(data) {
  return request.post('/delivery-orders', data)
}

export function getDeliveryTasks(params) {
  return request.get('/delivery-orders/tasks', { params })
}

export function acceptDeliveryOrder(id) {
  return request.put(`/delivery-orders/${id}/accept`)
}

export function updateDeliveryOrderStatus(id, status) {
  return request.put(`/delivery-orders/${id}/status`, { status })
}
