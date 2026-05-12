import { useState } from 'react'
import { changeOrderStatus } from '../api/orders'

// Transiciones válidas por estado
const TRANSITIONS = {
  PENDING:   ['VALIDATED', 'CANCELLED'],
  VALIDATED: ['PAID',      'CANCELLED'],
  PAID:      ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: [],
  CANCELLED: [],
}

// Clase de botón según el estado destino
const BTN_CLASS = {
  VALIDATED: 'btn btn-sm btn-primary',
  PAID:      'btn btn-sm btn-success',
  CONFIRMED: 'btn btn-sm btn-success',
  CANCELLED: 'btn btn-sm btn-danger',
}

export default function OrderStatusButton({ order, onUpdated }) {
  const [loading, setLoading] = useState(false)
  const transitions = TRANSITIONS[order.status] ?? []

  if (transitions.length === 0) return null

  const handleChange = async (newStatus) => {
    setLoading(true)
    try {
      const updated = await changeOrderStatus(order.id, newStatus)
      onUpdated(updated)
    } catch (err) {
      alert(`Error al cambiar estado: ${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="status-actions">
      {transitions.map((status) => (
        <button
          key={status}
          className={BTN_CLASS[status] ?? 'btn btn-sm btn-ghost'}
          onClick={() => handleChange(status)}
          disabled={loading}
        >
          → {status}
        </button>
      ))}
    </div>
  )
}
