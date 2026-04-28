// Servidor mock con json-server + ruta personalizada para cambio de estado
// Ejecutar con: node mock/server.cjs
const jsonServer = require('json-server')
const path = require('path')

const server = jsonServer.create()
const router = jsonServer.router(path.join(__dirname, 'db.json'))
const middlewares = jsonServer.defaults({ logger: false })

server.use(middlewares)
server.use(jsonServer.bodyParser)

// PUT /orders/:id/status → actualiza solo el estado del pedido
server.put('/orders/:id/status', (req, res) => {
  const db = router.db
  const order = db.get('orders').find({ id: req.params.id }).value()

  if (!order) {
    return res.status(404).json({ message: 'Pedido no encontrado' })
  }

  const updated = db
    .get('orders')
    .find({ id: req.params.id })
    .assign({ status: req.body.status, updatedAt: new Date().toISOString() })
    .write()

  console.log(`[MOCK] PUT /orders/${req.params.id}/status → ${req.body.status}`)
  res.json(updated)
})

server.use(router)

server.listen(3001, () => {
  console.log('Mock JSON Server corriendo en http://localhost:3001')
  console.log('Rutas disponibles:')
  console.log('  GET  /products')
  console.log('  POST /products')
  console.log('  PUT  /products/:id')
  console.log('  DELETE /products/:id')
  console.log('  GET  /customers')
  console.log('  POST /customers')
  console.log('  PUT  /customers/:id')
  console.log('  GET  /orders')
  console.log('  POST /orders')
  console.log('  PUT  /orders/:id/status  (custom)')
})
