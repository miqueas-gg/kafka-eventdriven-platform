import { useState, useEffect, useCallback } from 'react'
import { getCustomers, createCustomer, updateCustomer, deleteCustomer } from '../api/customers'
import Modal from '../components/Modal.jsx'

const EMPTY_FORM = {
  name: '',
  email: '',
  phone: '',
  street: '',
  city: '',
  country: 'España',
}

function formToPayload(form) {
  return {
    name:  form.name,
    email: form.email,
    phone: form.phone || null,
    address: (form.street || form.city || form.country)
      ? { street: form.street, city: form.city, country: form.country }
      : null,
  }
}

function customerToForm(c) {
  return {
    name:    c.name,
    email:   c.email,
    phone:   c.phone ?? '',
    street:  c.address?.street ?? '',
    city:    c.address?.city   ?? '',
    country: c.address?.country ?? 'España',
  }
}

export default function CustomersPage() {
  const [customers, setCustomers] = useState([])
  const [loading, setLoading]     = useState(true)
  const [pageError, setPageError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId]       = useState(null)
  const [form, setForm]           = useState(EMPTY_FORM)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving]       = useState(false)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setPageError(null)
      setCustomers(await getCustomers())
    } catch (e) {
      setPageError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setEditId(null)
    setFormError(null)
    setShowModal(true)
  }

  const openEdit = (c) => {
    setForm(customerToForm(c))
    setEditId(c.id)
    setFormError(null)
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditId(null)
    setFormError(null)
  }

  const handleChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      const payload = formToPayload(form)
      if (editId) {
        const updated = await updateCustomer(editId, payload)
        setCustomers((cs) => cs.map((c) => (c.id === editId ? updated : c)))
      } else {
        const created = await createCustomer(payload)
        setCustomers((cs) => [...cs, created])
      }
      closeModal()
    } catch (e) {
      setFormError(e.message)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este cliente? Sus pedidos asociados podrían verse afectados.')) return
    try {
      await deleteCustomer(id)
      setCustomers((cs) => cs.filter((c) => c.id !== id))
    } catch (e) {
      alert(`Error al eliminar: ${e.message}`)
    }
  }

  const formatAddress = (addr) => {
    if (!addr) return '—'
    return [addr.street, addr.city, addr.country].filter(Boolean).join(', ')
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header-info">
          <h2>Clientes</h2>
          <p>Registro de clientes del sistema</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>
          + Nuevo cliente
        </button>
      </div>

      <div className="page-body">
        {pageError && <div className="error-banner">Error al cargar clientes: {pageError}</div>}

        <div className="card">
          <div className="table-wrapper">
            {loading ? (
              <div className="loading">Cargando clientes...</div>
            ) : customers.length === 0 ? (
              <div className="empty-state">
                <strong>Sin clientes</strong>
                <p>Crea el primer cliente con el botón de arriba.</p>
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Teléfono</th>
                    <th>Dirección</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {customers.map((c) => (
                    <tr key={c.id}>
                      <td><strong>{c.name}</strong></td>
                      <td>{c.email}</td>
                      <td>{c.phone || '—'}</td>
                      <td style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {formatAddress(c.address)}
                      </td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => openEdit(c)}
                          >
                            Editar
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDelete(c.id)}
                          >
                            Eliminar
                          </button>
                        </div>
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
          title={editId ? 'Editar cliente' : 'Nuevo cliente'}
          onClose={closeModal}
          footer={
            <>
              <button className="btn btn-ghost" onClick={closeModal} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleSubmit} disabled={saving}>
                {saving ? 'Guardando...' : editId ? 'Guardar cambios' : 'Crear cliente'}
              </button>
            </>
          }
        >
          {formError && <div className="error-banner">{formError}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Nombre completo *</label>
                <input
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  required
                  placeholder="ej: Ana García López"
                />
              </div>

              <div className="form-group">
                <label>Email *</label>
                <input
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={handleChange}
                  required
                  placeholder="correo@ejemplo.com"
                />
              </div>

              <div className="form-group">
                <label>Teléfono</label>
                <input
                  name="phone"
                  value={form.phone}
                  onChange={handleChange}
                  placeholder="+34 600 000 000"
                />
              </div>

              <hr className="form-section-sep" />

              <div className="form-group full-width">
                <label>Calle</label>
                <input
                  name="street"
                  value={form.street}
                  onChange={handleChange}
                  placeholder="ej: Calle Gran Vía 45"
                />
              </div>

              <div className="form-group">
                <label>Ciudad</label>
                <input
                  name="city"
                  value={form.city}
                  onChange={handleChange}
                  placeholder="ej: Madrid"
                />
              </div>

              <div className="form-group">
                <label>País</label>
                <input
                  name="country"
                  value={form.country}
                  onChange={handleChange}
                  placeholder="España"
                />
              </div>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
