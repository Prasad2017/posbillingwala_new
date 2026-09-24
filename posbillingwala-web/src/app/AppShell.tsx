import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  BarChart3,
  Home,
  Package,
  Settings,
  Store,
  Table2,
  ShoppingBag,
  UtensilsCrossed,
  Warehouse,
  Lock,
  LogOut,
} from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { useI18n, type MessageKey } from '@/i18n'
import { OfflineBanner } from '@/shared/OfflineBanner'
import { mediaUrl } from '@/api/endpoints'

interface NavItem {
  to: string
  labelKey: MessageKey
  icon: typeof Home
  permission?: string
  licenceAllows?: (s: {
    fastBilling: boolean
    dineIn: boolean
    takeAway: boolean
    mess: boolean
  }) => boolean
}

const navItems: NavItem[] = [
  { to: '/', labelKey: 'home', icon: Home },
  {
    to: '/pos',
    labelKey: 'billing',
    icon: Store,
    permission: 'billing.create',
    licenceAllows: (s) => s.fastBilling,
  },
  {
    to: '/tables',
    labelKey: 'tables',
    icon: Table2,
    permission: 'table.view',
    licenceAllows: (s) => s.dineIn,
  },
  {
    to: '/takeaway',
    labelKey: 'takeaway',
    icon: ShoppingBag,
    permission: 'takeaway.view',
    licenceAllows: (s) => s.takeAway,
  },
  {
    to: '/mess',
    labelKey: 'mess',
    icon: UtensilsCrossed,
    permission: 'mess.view',
    licenceAllows: (s) => s.mess,
  },
  { to: '/masters', labelKey: 'masters', icon: Package, permission: 'product.view' },
  { to: '/inventory', labelKey: 'inventory', icon: Warehouse, permission: 'inventory.view' },
  { to: '/reports', labelKey: 'reports', icon: BarChart3, permission: 'report.view' },
  { to: '/settings', labelKey: 'settings', icon: Settings },
]

export function AppShell() {
  const navigate = useNavigate()
  const t = useI18n((s) => s.t)
  const shopName = useAuthStore((s) => s.shopName)
  const shopImage = useAuthStore((s) => s.shopImage)
  const staff = useAuthStore((s) => s.staff)
  const softLogout = useAuthStore((s) => s.softLogout)
  const hardLogout = useAuthStore((s) => s.hardLogout)
  const hasPermission = useAuthStore((s) => s.hasPermission)
  const fastBilling = useAuthStore((s) => s.fastBilling)
  const dineIn = useAuthStore((s) => s.dineIn)
  const takeAway = useAuthStore((s) => s.takeAway)
  const mess = useAuthStore((s) => s.mess)
  const logo = mediaUrl(shopImage)

  const visible = navItems.filter((item) => {
    if (item.licenceAllows && !item.licenceAllows({ fastBilling, dineIn, takeAway, mess })) {
      return false
    }
    return hasPermission(item.permission)
  })

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          {logo ? <img src={logo} alt="" /> : <div style={{ width: 36, height: 36, borderRadius: 8, background: '#fff3' }} />}
          <div>
            <strong>{shopName || t('appName')}</strong>
            <span>{staff?.staffName || 'Owner'}</span>
          </div>
        </div>
        {visible.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              <Icon size={18} />
              <span>{t(item.labelKey)}</span>
            </NavLink>
          )
        })}
      </aside>
      <div className="main-area">
        <OfflineBanner />
        <header className="topbar no-print">
          <div className="muted">Online POS</div>
          <div className="row">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                softLogout()
                navigate('/mpin')
              }}
            >
              <Lock size={16} /> {t('softLogout')}
            </button>
            <button
              type="button"
              className="btn btn-danger"
              onClick={() => {
                hardLogout()
                navigate('/login')
              }}
            >
              <LogOut size={16} /> {t('logout')}
            </button>
          </div>
        </header>
        <main className="page">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
