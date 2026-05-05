import client from './client'

export const getOrders = () =>
  client.get('/orders').then((r) => (Array.isArray(r.data) ? r.data : r.data.content ?? []))

export const createOrder = (data) =>
  client.post('/orders', data).then((r) => r.data)

// body: { status: 'VALIDATED' | 'PAID' | 'CONFIRMED' | 'CANCELLED', reason?: string }
export const changeOrderStatus = (id, status, reason = '') =>
  client.patch(`/orders/${id}/status`, { 
    newStatus: status, 
    reason }).then((r) => r.data)
