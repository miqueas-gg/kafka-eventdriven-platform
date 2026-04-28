import client from './client'

export const getProducts = () =>
  client.get('/products').then((r) => (Array.isArray(r.data) ? r.data : r.data.content ?? []))

export const createProduct = (data) =>
  client.post('/products', data).then((r) => r.data)

export const updateProduct = (id, data) =>
  client.put(`/products/${id}`, data).then((r) => r.data)

export const deleteProduct = (id) =>
  client.delete(`/products/${id}`)
