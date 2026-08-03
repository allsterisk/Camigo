import request from '../utils/request'

export function createParcel(data) {
  return request.post('/parcels', data)
}

export function getParcelPage(params) {
  return request.get('/parcels', { params })
}

export function getMyParcels(params) {
  return request.get('/parcels/my', { params })
}

export function getParcel(id) {
  return request.get(`/parcels/${id}`)
}

export function updateParcelStatus(id, data) {
  return request.put(`/parcels/${id}/status`, data)
}

export function deleteParcel(id) {
  return request.delete(`/parcels/${id}`)
}
