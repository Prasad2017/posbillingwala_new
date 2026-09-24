import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Store,
  Table2,
  ShoppingBag,
  UtensilsCrossed,
  Package,
  BarChart3,
} from 'lucide-react'
import { catalogApi } from '@/api/services'
import { useAuthStore, useUserId } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { PageHeader, Money, LoadingBlock } from '@/shared/ui'

export function HomePage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const shopName = useAuthStore((s) => s.shopName)
  const todaySaleData = useAuthStore((s) => s.todaySaleData)
  const totalSaleData = useAuthStore((s) => s.totalSaleData)
  const hasPermission = useAuthStore((s) => s.hasPermission)
  const fastBilling = useAuthStore((s) => s.fastBilling)
  const dineIn = useAuthStore((s) => s.dineIn)
  const takeAway = useAuthStore((s) => s.takeAway)
  const mess = useAuthStore((s) => s.mess)

  const catalog = useQuery({
    queryKey: ['home-catalog', userId],
    enabled: Boolean(userId),
    queryFn: async () => {
      const id = userId!
      const [categories, products, combos, portions] = await Promise.all([
        catalogApi.getCategories(id),
        catalogApi.getProducts(id),
        catalogApi.getCombos(id),
        catalogApi.getPortions(id),
      ])
      return {
        categories: categories.length,
        products: products.length,
        combos: combos.length,
        portions: portions.length,
      }
    },
  })

  const today = Number(todaySaleData ?? 0) || 0
  const total = Number(totalSaleData ?? 0) || 0

  const tiles = [
    {
      to: '/pos',
      label: t('billing'),
      icon: Store,
      show: fastBilling && hasPermission('billing.create'),
    },
    {
      to: '/tables',
      label: t('tables'),
      icon: Table2,
      show: dineIn && hasPermission('table.view'),
    },
    {
      to: '/takeaway',
      label: t('takeaway'),
      icon: ShoppingBag,
      show: takeAway && hasPermission('takeaway.view'),
    },
    {
      to: '/mess',
      label: t('mess'),
      icon: UtensilsCrossed,
      show: mess && hasPermission('mess.view'),
    },
    {
      to: '/masters',
      label: t('masters'),
      icon: Package,
      show: hasPermission('product.view'),
    },
    {
      to: '/reports',
      label: t('reports'),
      icon: BarChart3,
      show: hasPermission('report.view'),
    },
  ].filter((x) => x.show)

  return (
    <div>
      <PageHeader
        title={shopName || t('appName')}
        subtitle="Dashboard overview"
      />

      <div className="grid-kpi" style={{ marginBottom: '1.25rem' }}>
        <div className="card kpi">
          <div className="label">{t('todaySales')}</div>
          <div className="value">
            <Money value={today} />
          </div>
        </div>
        <div className="card kpi">
          <div className="label">{t('totalSales')}</div>
          <div className="value">
            <Money value={total} />
          </div>
        </div>
        <div className="card kpi">
          <div className="label">Products</div>
          <div className="value">
            {catalog.isLoading ? '…' : (catalog.data?.products ?? 0)}
          </div>
        </div>
        <div className="card kpi">
          <div className="label">Categories</div>
          <div className="value">
            {catalog.isLoading ? '…' : (catalog.data?.categories ?? 0)}
          </div>
        </div>
      </div>

      {catalog.isLoading ? <LoadingBlock /> : null}

      <div className="table-grid">
        {tiles.map((tile) => {
          const Icon = tile.icon
          return (
            <Link key={tile.to} to={tile.to} className="table-tile">
              <Icon size={22} />
              <strong style={{ display: 'block', marginTop: '0.5rem' }}>
                {tile.label}
              </strong>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
