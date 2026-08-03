import request from '../utils/request'

export function registerUser(data) {
  return request.post('/users/register', data)
}

export function loginUser(data) {
  return request.post('/users/login', data)
}

export function getCurrentUser() {
  return request.get('/users/info')
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}
