import { useState, useEffect, useCallback } from 'react'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api/products'
import Modal from '../components/Modal.jsx'
import StatusBadge from '../components/StatusBadge.jsx'

const EMPTY_FORM = {
  name: '',
  description: '',
  price: '',
  stock: '',
  category: '',
  status: 'ACTIVE',
}

export default function ProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading]   = useState(true)
  const [pageError, setPageError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editId, setEditId]     = useState(null)
  const [form, setForm]         = useState(EMPTY_FORM)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving]     = useState(false)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setPageError(null)
      setProducts(await getProducts())
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

  const openEdit = (p) => {
    setForm({
      name: p.name,
      description: p.description ?? '',
      price: p.price,
      stock: p.stock,
      category: p.category ?? '',
      status: p.status,
    })
    setEditId(p.id)
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
      const payload = {
        ...form,
        price: parseFloat(form.price),
        stock: parseInt(form.stock, 10),
      }
      if (editId) {
        const updated = await updateProduct(editId, payload)
        setProducts((ps) => ps.map((p) => (p.id === editId ? updated : p)))
      } else {
        const created = await createProduct(payload)
        setProducts((ps) => [...ps, created])
      }
      closeModal()
    } catch (e) {
      setFormError(e.message)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este producto? Esta acción no se puede deshacer.')) return
    try {
      await deleteProduct(id)
      setProducts((ps) => ps.filter((p) => p.id !== id))
    } catch (e) {
      alert(`Error al eliminar: ${e.message}`)
    }
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header-info">
          <h2>Productos</h2>
          <p>Catálogo completo de productos — cada cambio publica un evento a Kafka</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>
          + Nuevo producto
        </button>
      </div>

      <div className="page-body">
        {pageError && <div className="error-banner">Error al cargar productos: {pageError}</div>}

        <div className="card">
          <div className="table-wrapper">
            {loading ? (
              <div className="loading">Cargando productos...</div>
            ) : products.length === 0 ? (
              <div className="empty-state">
                <strong>Sin productos</strong>
                <p>Crea el primer producto con el botón de arriba.</p>
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Categoría</th>
                    <th>Precio</th>
                    <th>Stock</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((p) => (
                    <tr key={p.id}>
                      <td className="cell-name">
                        <strong>{p.name}</strong>
                        {p.description && <span>{p.description}</span>}
                      </td>
                      <td>{p.category || '—'}</td>
                      <td className="cell-amount">{Number(p.price).toFixed(2)} €</td>
                      <td>{p.stock}</td>
                      <td><StatusBadge status={p.status} /></td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => openEdit(p)}
                          >
                            Editar
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDelete(p.id)}
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
          title={editId ? 'Editar producto' : 'Nuevo producto'}
          onClose={closeModal}
          footer={
            <>
              <button className="btn btn-ghost" onClick={closeModal} disabled={saving}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleSubmit} disabled={saving}>
                {saving ? 'Guardando...' : editId ? 'Guardar cambios' : 'Crear producto'}
              </button>
            </>
          }
        >
          {formError && <div className="error-banner">{formError}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group full-width">
                <label>Nombre *</label>
                <input
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  required
                  placeholder="ej: Laptop Pro 15"
                />
              </div>

              <div className="form-group full-width">
                <label>Descripción</label>
                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  placeholder="Breve descripción del producto..."
                />
              </div>

              <div className="form-group">
                <label>Precio (€) *</label>
                <input
                  name="price"
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.price}
                  onChange={handleChange}
                  required
                  placeholder="0.00"
                />
              </div>

              <div className="form-group">
                <label>Stock *</label>
                <input
                  name="stock"
                  type="number"
                  min="0"
                  value={form.stock}
                  onChange={handleChange}
                  required
                  placeholder="0"
                />
              </div>

              <div className="form-group">
                <label>Categoría</label>
                <input
                  name="category"
                  value={form.category}
                  onChange={handleChange}
                  placeholder="ej: Electrónica"
                />
              </div>

              <div className="form-group">
                <label>Estado</label>
                <select name="status" value={form.status} onChange={handleChange}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                  <option value="OUT_OF_STOCK">OUT_OF_STOCK</option>
                </select>
              </div>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
