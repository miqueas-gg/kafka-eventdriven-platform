import { NavLink, Outlet } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/products',  label: 'Productos',  icon: '📦' },
  { to: '/customers', label: 'Clientes',   icon: '👤' },
  { to: '/orders',    label: 'Pedidos',    icon: '🛒' },
]

export default function Layout() {
  return (
    <div className="app-layout">
      <nav className="sidebar">
        <div className="sidebar-logo">
          <h1>Kafka E-Commerce</h1>
          <p>Event-driven platform</p>
        </div>
        <div className="sidebar-nav">
          {NAV_ITEMS.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => (isActive ? 'active' : '')}
            >
              <span className="nav-icon">{icon}</span>
              {label}
            </NavLink>
          ))}
        </div>
      </nav>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
