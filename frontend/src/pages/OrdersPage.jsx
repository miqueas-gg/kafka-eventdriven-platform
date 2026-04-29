import { useState, useEffect, useCallback } from 'react'
import { getOrders, createOrder } from '../api/orders'
import { getCustomers } from '../api/customers'
import { getProducts } from '../api/products'
import Modal from '../components/Modal.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import OrderStatusButton from '../components/OrderStatusButton.jsx'

const EMPTY_ITEM = { productId: '', quantity: 1 }
const EMPTY_FORM = { customerId: '', notes: '', items: [{ ...EMPTY_ITEM }] }

export default function OrdersPage() {
  const [orders, setOrders]       = useState([])
  const [customers, setCustomers] = useState([])
  const [products, setProducts]   = useState([])
  const [loading, setLoading]     = useState(true)
  const [pageError, setPageError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [form, setForm]           = useState(EMPTY_FORM)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving]       = useState(false)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setPageError(null)
      const [o, c, p] = await Promise.all([getOrders(), getCustomers(), getProducts()])
      setOrders(o)
      setCustomers(c)
      setProducts(p)
    } catch (e) {
      setPageError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  // --- Helpers para el formulario de pedido ---

  const getProduct = (productId) => products.find((p) => p.id === productId)

  const computeTotal = (items) =>
    items.reduce((sum, item) => {
      const p = getProduct(item.productId)
      return sum + (p ? p.price * (parseInt(item.quantity, 10) || 0) : 0)
    }, 0)

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setFormError(null)
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setFormError(null)
  }

  const handleFormChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleItemChange = (index, field, value) => {
    setForm((f) => {
      const items = [...f.items]
      items[index] = { ...items[index], [field]: value }
      return { ...f, items }
    })
  }

  const addItem = () =>
    setForm((f) => ({ ...f, items: [...f.items, { ...EMPTY_ITEM }] }))

  const removeItem = (index) =>
    setForm((f) => ({ ...f, items: f.items.filter((_, i) => i !== index) }))

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Validaciones básicas
    if (!form.customerId) { setFormError('Selecciona un cliente.'); return }
    if (form.items.length === 0) { setFormError('Añade al menos un producto.'); return }
    const hasEmpty = form.items.some((i) => !i.productId || !(parseInt(i.quantity, 10) > 0))
    if (hasEmpty) { setFormError('Todos los productos deben tener cantidad válida.'); return }

    setSaving(true)
    setFormError(null)

    try {
      // Enriquecer items con datos del catálogo local (necesario para que el mock almacene datos completos)
      const customer = customers.find((c) => c.id === form.customerId)
      const enrichedItems = form.items.map((item) => {
        const p = getProduct(item.productId)
        const qty = parseInt(item.quantity, 10)
        return {
          productId:   item.productId,
          productName: p?.name ?? '',
          quantity:    qty,
          unitPrice:   p?.price ?? 0,
          subtotal:    (p?.price ?? 0) * qty,
        }
      })

      const payload = {
        customerId:   form.customerId,
        customerName: customer?.name ?? '',
        notes:        form.notes || null,
        totalAmount:  computeTotal(form.items),
        status:       'PENDING',
        items:        enrichedItems,
      }

      const created = await createOrder(payload)
      setOrders((os) => [created, ...os])
      closeModal()
    } catch (e) {
      setFormError(e.message)
    } finally {
      setSaving(false)
    }
  }

  // Callback para actualizar el estado de un pedido en local tras cambio de status
  const handleOrderUpdated = (updated) =>{
    setOrders((os) => os.map((o) => (o.id === updated.id ? updated : o)))
    load();
  }
  const formatDate = (iso) =>
    iso ? new Date(iso).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' }) : '—'

  const total = computeTotal(form.items)

  return (
    <>
      <div className="page-header">
        <div className="page-header-info">
          <h2>Pedidos</h2>
          <p>Gestión de pedidos — cada cambio de estado publica un evento ORDER_STATUS_CHANGED</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>
          + Nuevo pedido
        </button>
      </div>

      <div className="page-body">
        {pageError && <div className="error-banner">Error al cargar datos: {pageError}</div>}

        <div className="card">
          <div className="table-wrapper">
            {loading ? (
              <div className="loading">Cargando pedidos...</div>
            ) : orders.length === 0 ? (
              <div className="empty-state">
                <strong>Sin pedidos</strong>
                <p>Crea el primer pedido con el botón de arriba.</p>
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Cliente</th>
                    <th>Productos</th>
                    <th>Total</th>
                    <th>Estado</th>
                    <th>Fecha</th>
                    <th>Cambiar estado</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((o) => (
                    <tr key={o.id}>
                      <td>
                        <code style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                          {o.id?.slice(0, 8)}…
                        </code>
                      </td>
                      <td><strong>{o.customerName ?? o.customerId}</strong></td>
                      <td>
                        <div className="cell-name">
                          <strong>{o.items?.length ?? 0} línea(s)</strong>
                          <span className="order-items-preview">
                            {o.items?.map((i) => `${i.productName} ×${i.quantity}`).join(' · ')}
                          </span>
                        </div>
                      </td>
                      <td className="cell-amount">{Number(o.totalAmount).toFixed(2)} €</td>
                      <td><StatusBadge status={o.status} /></td>
                      <td style={{ whiteSpace: 'nowrap', fontSize: 12, color: 'var(--text-muted)' }}>
                        {formatDate(o.createdAt)}
                      </td>
                      <td>
                        <OrderStatusButton order={o} onUpdated={handleOrderUpdated} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {showModal && (
        <Modal
          title="Nuevo pedido"
          onClose={closeModal}
          size="modal-lg"
          footer={
            <>
              <button className="btn btn-ghost" onClick={closeModal} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleSubmit} disabled={saving}>
                {saving ? 'Creando...' : 'Crear pedido'}
              </button>
            </>
          }
        >
          {formError && <div className="error-banner">{formError}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label>Cliente *</label>
                <select
                  name="customerId"
                  value={form.customerId}
                  onChange={handleFormChange}
                  required
                >
                  <option value="">— Selecciona un cliente —</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Notas</label>
                <input
                  name="notes"
                  value={form.notes}
                  onChange={handleFormChange}
                  placeholder="Instrucciones de entrega, etc."
                />
              </div>
            </div>

            {/* Líneas de pedido */}
            <p className="items-section-title">Productos del pedido</p>

            <div className="items-list">
              {/* Cabecera */}
              <div className="item-row" style={{ color: 'var(--text-muted)', fontSize: 11, fontWeight: 700, textTransform: 'uppercase' }}>
                <span>Producto</span>
                <span>Cant.</span>
                <span>Precio unit.</span>
                <span style={{ textAlign: 'right' }}>Subtotal</span>
                <span />
              </div>

              {form.items.map((item, idx) => {
                const p = getProduct(item.productId)
                const qty = parseInt(item.quantity, 10) || 0
                const subtotal = p ? p.price * qty : 0
                return (
                  <div className="item-row" key={idx}>
                    <select
                      value={item.productId}
                      onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                      required
                    >
                      <option value="">— Producto —</option>
                      {products.map((prod) => (
                        <option key={prod.id} value={prod.id}>
                          {prod.name}
                        </option>
                      ))}
                    </select>

                    <input
                      type="number"
                      min="1"
                      value={item.quantity}
                      onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                      required
                    />

                    <input
                      value={p ? `${Number(p.price).toFixed(2)} €` : '—'}
                      disabled
                    />

                    <span className="item-subtotal">
                      {p ? `${subtotal.toFixed(2)} €` : '—'}
                    </span>

                    <button
                      type="button"
                      className="item-remove"
                      onClick={() => removeItem(idx)}
                      disabled={form.items.length === 1}
                      title="Quitar línea"
                    >
                      ×
                    </button>
                  </div>
                )
              })}
            </div>

            <div style={{ marginTop: 10 }}>
              <button type="button" className="btn btn-ghost btn-sm" onClick={addItem}>
                + Añadir producto
              </button>
            </div>

            <div className="items-total-row">
              <span className="items-total-label">Total del pedido:</span>
              <span className="items-total-amount">{total.toFixed(2)} €</span>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
