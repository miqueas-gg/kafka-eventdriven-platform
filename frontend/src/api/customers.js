import client from './client'

export const getCustomers = () =>
  client.get('/customers').then((r) => (Array.isArray(r.data) ? r.data : r.data.content ?? []))

export const createCustomer = (data) =>
  client.post('/customers', data).then((r) => r.data)

export const updateCustomer = (id, data) =>
  client.put(`/customers/${id}`, data).then((r) => r.data)

export const deleteCustomer = (id) =>
  client.delete(`/customers/${id}`)
