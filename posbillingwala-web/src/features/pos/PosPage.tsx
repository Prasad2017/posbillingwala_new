import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Minus, Plus, Trash2, Search } from 'lucide-react'
import type { JsonMap } from '@/api/client'
import { catalogApi } from '@/api/services'
import { useUserId } from '@/stores/authStore'
import {
  useCartStore,
  defaultFastBillingSession,
} from '@/stores/cartStore'
import { useI18n } from '@/i18n'
import { PageHeader, EmptyState, Money, LoadingBlock, ErrorBlock } from '@/shared/ui'

function str(row: JsonMap, ...keys: string[]): string {
  for (const k of keys) {
    const v = row[k]
    if (v != null && String(v).trim()) return String(v)
  }
  return ''
}

function num(row: JsonMap, ...keys: string[]): number {
  for (const k of keys) {
    const v = row[k]
    if (v != null && v !== '') {
      const n = Number(v)
      if (!Number.isNaN(n)) return n
    }
  }
  return 0
}

interface PortionChoice {
  portionId: string
  portionName: string
  unitPrice: number
}

export function PosPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const location = useLocation()
  const userId = useUserId()
  const lines = useCartStore((s) => s.lines)
  const session = useCartStore((s) => s.session)
  const addLine = useCartStore((s) => s.addLine)
  const updateQty = useCartStore((s) => s.updateQty)
  const removeLine = useCartStore((s) => s.removeLine)
  const resetSession = useCartStore((s) => s.resetSession)
  const totals = useCartStore((s) => s.totals)

  const [categoryId, setCategoryId] = useState<string>('all')
  const [search, setSearch] = useState('')
  const [portionModal, setPortionModal] = useState<{
    productId: string
    name: string
    gstPercent: number
    portions: PortionChoice[]
  } | null>(null)

  useEffect(() => {
    if (location.pathname === '/pos') {
      resetSession(defaultFastBillingSession)
    }
  }, [location.pathname, resetSession])

  const catalog = useQuery({
    queryKey: ['pos-catalog', userId],
    enabled: Boolean(userId),
    queryFn: async () => {
      const id = userId!
      const [categories, products, portions, combos] = await Promise.all([
        catalogApi.getCategories(id),
        catalogApi.getProducts(id),
        catalogApi.getPortions(id),
        catalogApi.getCombos(id),
      ])
      return { categories, products, portions, combos }
    },
  })

  const portionsByProduct = useMemo(() => {
    const map = new Map<string, PortionChoice[]>()
    for (const p of catalog.data?.portions ?? []) {
      const productId = str(p, 'productId')
      if (!productId) continue
      const list = map.get(productId) ?? []
      list.push({
        portionId: str(p, 'portionId'),
        portionName: str(p, 'portionName') || 'Regular',
        unitPrice: num(p, 'portionPrice'),
      })
      map.set(productId, list)
    }
    return map
  }, [catalog.data?.portions])

  const filteredProducts = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (catalog.data?.products ?? []).filter((p) => {
      const status = str(p, 'productStatus', 'productDeletedStatus')
      if (status === '0' && str(p, 'productDeletedStatus') === '1') return false
      if (str(p, 'productDeletedStatus') === '1') return false
      const cat = str(p, 'categoryId')
      if (categoryId !== 'all' && cat !== categoryId) return false
      if (!q) return true
      return str(p, 'productName').toLowerCase().includes(q)
    })
  }, [catalog.data?.products, categoryId, search])

  const filteredCombos = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (categoryId !== 'all' && categoryId !== 'combo') return []
    return (catalog.data?.combos ?? []).filter((c) => {
      if (str(c, 'comboDeletedStatus') === '1') return false
      if (!q) return true
      return str(c, 'comboName').toLowerCase().includes(q)
    })
  }, [catalog.data?.combos, categoryId, search])

  function addProduct(product: JsonMap) {
    const productId = str(product, 'productId')
    const name = str(product, 'productName')
    const gstPercent =
      num(product, 'productCGST') + num(product, 'productSGST')
    const portions = portionsByProduct.get(productId) ?? []
    if (portions.length > 1) {
      setPortionModal({ productId, name, gstPercent, portions })
      return
    }
    const portion = portions[0]
    addLine({
      kind: 'product',
      productId,
      name,
      portionId: portion?.portionId,
      portionName: portion?.portionName,
      unitPrice: portion?.unitPrice ?? num(product, 'productPrice'),
      qty: 1,
      gstPercent,
    })
  }

  function addCombo(combo: JsonMap) {
    addLine({
      kind: 'combo',
      comboId: str(combo, 'comboId'),
      name: str(combo, 'comboName'),
      unitPrice: num(combo, 'comboPrice', 'price'),
      qty: 1,
      gstPercent: num(combo, 'gstPercent', 'comboGst'),
    })
  }

  const cartTotals = totals()

  return (
    <div>
      <PageHeader title={session.title} subtitle={t('billing')} />

      {catalog.isLoading ? <LoadingBlock /> : null}
      {catalog.isError ? (
        <ErrorBlock message="Failed to load menu" />
      ) : null}

      <div className="pos-layout">
        <div className="stack">
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="menu-search">
              <Search size={14} style={{ marginRight: 4 }} />
              {t('search')}
            </label>
            <input
              id="menu-search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('search')}
            />
          </div>

          <div className="chip-row">
            <button
              type="button"
              className={`chip${categoryId === 'all' ? ' active' : ''}`}
              onClick={() => setCategoryId('all')}
            >
              All
            </button>
            {(catalog.data?.categories ?? []).map((c) => {
              const id = str(c, 'categoryId')
              return (
                <button
                  key={id}
                  type="button"
                  className={`chip${categoryId === id ? ' active' : ''}`}
                  onClick={() => setCategoryId(id)}
                >
                  {str(c, 'categoryName')}
                </button>
              )
            })}
            <button
              type="button"
              className={`chip${categoryId === 'combo' ? ' active' : ''}`}
              onClick={() => setCategoryId('combo')}
            >
              Combos
            </button>
          </div>

          <div className="menu-grid">
            {filteredProducts.map((p) => {
              const id = str(p, 'productId')
              const portions = portionsByProduct.get(id) ?? []
              const price =
                portions[0]?.unitPrice ?? num(p, 'productPrice')
              return (
                <button
                  key={id}
                  type="button"
                  className="menu-item"
                  onClick={() => addProduct(p)}
                >
                  <strong>{str(p, 'productName')}</strong>
                  <div className="price">
                    <Money value={price} />
                    {portions.length > 1 ? ' +' : ''}
                  </div>
                </button>
              )
            })}
            {filteredCombos.map((c) => {
              const id = str(c, 'comboId')
              return (
                <button
                  key={`combo-${id}`}
                  type="button"
                  className="menu-item"
                  onClick={() => addCombo(c)}
                >
                  <strong>{str(c, 'comboName')}</strong>
                  <div className="price">
                    <Money value={num(c, 'comboPrice', 'price')} />
                  </div>
                </button>
              )
            })}
          </div>

          {!catalog.isLoading &&
          filteredProducts.length === 0 &&
          filteredCombos.length === 0 ? (
            <EmptyState message={t('noData')} />
          ) : null}
        </div>

        <aside className="card stack">
          <h3 style={{ margin: 0 }}>{t('cart')}</h3>
          {lines.length === 0 ? (
            <p className="muted">{t('emptyCart')}</p>
          ) : (
            lines.map((line) => (
              <div key={line.id} className="row space-between">
                <div style={{ flex: 1, minWidth: 0 }}>
                  <strong style={{ display: 'block' }}>{line.name}</strong>
                  {line.portionName ? (
                    <span className="muted" style={{ fontSize: '0.8rem' }}>
                      {line.portionName}
                    </span>
                  ) : null}
                  <div>
                    <Money value={line.unitPrice * line.qty} />
                  </div>
                </div>
                <div className="row">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ padding: '0.35rem 0.5rem' }}
                    onClick={() => updateQty(line.id, line.qty - 1)}
                  >
                    <Minus size={14} />
                  </button>
                  <span>{line.qty}</span>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ padding: '0.35rem 0.5rem' }}
                    onClick={() => updateQty(line.id, line.qty + 1)}
                  >
                    <Plus size={14} />
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => removeLine(line.id)}
                    aria-label="Remove"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))
          )}

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />
          <div className="row space-between">
            <span className="muted">Subtotal</span>
            <Money value={cartTotals.subTotal} />
          </div>
          <div className="row space-between">
            <span className="muted">GST</span>
            <Money value={cartTotals.gstTotal} />
          </div>
          <div className="row space-between">
            <strong>Total</strong>
            <strong>
              <Money value={cartTotals.grandTotal} />
            </strong>
          </div>

          <button
            type="button"
            className="btn"
            disabled={lines.length === 0}
            onClick={() => navigate(session.paymentRoute)}
          >
            {t('checkout')}
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            disabled={lines.length === 0}
            onClick={() => navigate('/pos/kot')}
          >
            KOT preview
          </button>
        </aside>
      </div>

      {portionModal ? (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(15,27,45,0.45)',
            display: 'grid',
            placeItems: 'center',
            zIndex: 80,
            padding: '1rem',
          }}
        >
          <div className="card" style={{ width: 'min(360px, 100%)' }}>
            <h3 style={{ marginTop: 0 }}>{portionModal.name}</h3>
            <p className="muted">Choose portion</p>
            <div className="stack">
              {portionModal.portions.map((p) => (
                <button
                  key={p.portionId}
                  type="button"
                  className="btn btn-secondary"
                  style={{ justifyContent: 'space-between' }}
                  onClick={() => {
                    addLine({
                      kind: 'product',
                      productId: portionModal.productId,
                      name: portionModal.name,
                      portionId: p.portionId,
                      portionName: p.portionName,
                      unitPrice: p.unitPrice,
                      qty: 1,
                      gstPercent: portionModal.gstPercent,
                    })
                    setPortionModal(null)
                  }}
                >
                  <span>{p.portionName}</span>
                  <Money value={p.unitPrice} />
                </button>
              ))}
            </div>
            <button
              type="button"
              className="btn btn-ghost"
              style={{ marginTop: '0.75rem', width: '100%' }}
              onClick={() => setPortionModal(null)}
            >
              {t('cancel')}
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
