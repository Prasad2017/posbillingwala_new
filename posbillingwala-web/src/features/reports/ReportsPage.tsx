import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import type { JsonMap } from '@/api/client'
import { invoiceApi } from '@/api/services'
import { useAuthStore, useUserId } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { PageHeader, LoadingBlock, EmptyState, Money } from '@/shared/ui'

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

type View = 'hub' | 'dashboard' | 'invoices' | 'overview'

const DEFAULT_REPORT_PIN = '9082'
const UNLOCK_KEY = 'pb-report-unlocked'

export function ReportsPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const reportPin = useAuthStore((s) => s.reportPin)
  const navigate = useNavigate()
  const [unlocked, setUnlocked] = useState(() => {
    try {
      return sessionStorage.getItem(UNLOCK_KEY) === '1'
    } catch {
      return false
    }
  })
  const [pin, setPin] = useState('')
  const [pinError, setPinError] = useState<string | null>(null)
  const [view, setView] = useState<View>('hub')

  const expected = (reportPin && reportPin.trim()) || DEFAULT_REPORT_PIN

  function unlock(e: FormEvent) {
    e.preventDefault()
    if (pin.trim() === expected) {
      setUnlocked(true)
      setPinError(null)
      try {
        sessionStorage.setItem(UNLOCK_KEY, '1')
      } catch {
        /* ignore */
      }
    } else {
      setPinError('Incorrect PIN')
    }
  }

  function openInvoice(id: string) {
    navigate(`/reports/invoice/${encodeURIComponent(id)}`)
  }

  if (!unlocked) {
    return (
      <div>
        <PageHeader title={t('reports')} subtitle="Enter report PIN" />
        <form className="card" style={{ maxWidth: 360 }} onSubmit={unlock}>
          <div className="field">
            <label htmlFor="rpin">{t('reportPin')}</label>
            <input
              id="rpin"
              type="password"
              inputMode="numeric"
              value={pin}
              onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))}
              required
            />
          </div>
          {pinError ? <p className="error-text">{pinError}</p> : null}
          <button type="submit" className="btn" style={{ width: '100%' }}>
            {t('unlock')}
          </button>
        </form>
      </div>
    )
  }

  return (
    <div>
      <PageHeader
        title={t('reports')}
        actions={
          view !== 'hub' ? (
            <button type="button" className="btn btn-secondary" onClick={() => setView('hub')}>
              Hub
            </button>
          ) : null
        }
      />

      {view === 'hub' ? (
        <div className="table-grid">
          <button type="button" className="table-tile" onClick={() => setView('dashboard')}>
            <strong>Dashboard</strong>
            <div className="muted" style={{ marginTop: '0.35rem' }}>
              Sales report + recent invoices
            </div>
          </button>
          <button type="button" className="table-tile" onClick={() => setView('invoices')}>
            <strong>Invoices</strong>
            <div className="muted" style={{ marginTop: '0.35rem' }}>
              Full invoice list
            </div>
          </button>
          <button type="button" className="table-tile" onClick={() => setView('overview')}>
            <strong>Sales overview</strong>
            <div className="muted" style={{ marginTop: '0.35rem' }}>
              POS sales report summary
            </div>
          </button>
        </div>
      ) : null}

      {view === 'dashboard' || view === 'overview' ? (
        <SalesDashboard
          userId={userId}
          showInvoices={view === 'dashboard'}
          onOpenInvoice={openInvoice}
        />
      ) : null}

      {view === 'invoices' ? (
        <InvoiceList userId={userId} onOpenInvoice={openInvoice} />
      ) : null}
    </div>
  )
}

function SalesDashboard({
  userId,
  showInvoices,
  onOpenInvoice,
}: {
  userId: string | null
  showInvoices: boolean
  onOpenInvoice: (id: string) => void
}) {
  const salesQ = useQuery({
    queryKey: ['pos-sales-report', userId],
    enabled: Boolean(userId),
    queryFn: () => invoiceApi.getPosSalesReport(userId!),
  })

  const invQ = useQuery({
    queryKey: ['invoice-list', userId],
    enabled: Boolean(userId) && showInvoices,
    queryFn: () => invoiceApi.getInvoiceList(userId!),
  })

  const summary = useMemo(() => {
    const d = salesQ.data ?? {}
    return {
      today: num(d, 'todaySale', 'todaySales', 'todaySaleData', 'totalToday'),
      total: num(d, 'totalSale', 'totalSales', 'totalSaleData', 'grandTotal'),
      bills: num(d, 'billCount', 'invoiceCount', 'totalBills'),
      raw: d,
    }
  }, [salesQ.data])

  return (
    <div className="stack">
      {salesQ.isLoading ? <LoadingBlock /> : null}
      <div className="grid-kpi">
        <div className="card kpi">
          <div className="label">Today</div>
          <div className="value">
            <Money value={summary.today} />
          </div>
        </div>
        <div className="card kpi">
          <div className="label">Total</div>
          <div className="value">
            <Money value={summary.total} />
          </div>
        </div>
        <div className="card kpi">
          <div className="label">Bills</div>
          <div className="value">{summary.bills || '—'}</div>
        </div>
      </div>

      {salesQ.data ? (
        <details className="card">
          <summary>Raw sales report</summary>
          <pre style={{ fontSize: 11, overflow: 'auto' }}>
            {JSON.stringify(salesQ.data, null, 2)}
          </pre>
        </details>
      ) : null}

      {showInvoices ? (
        <>
          <h3 style={{ margin: '0.5rem 0' }}>Recent invoices</h3>
          {invQ.isLoading ? <LoadingBlock /> : null}
          <InvoiceTable rows={invQ.data ?? []} onOpen={onOpenInvoice} />
        </>
      ) : null}
    </div>
  )
}

function InvoiceList({
  userId,
  onOpenInvoice,
}: {
  userId: string | null
  onOpenInvoice: (id: string) => void
}) {
  const t = useI18n((s) => s.t)
  const q = useQuery({
    queryKey: ['invoice-list', userId],
    enabled: Boolean(userId),
    queryFn: () => invoiceApi.getInvoiceList(userId!),
  })

  if (q.isLoading) return <LoadingBlock />
  if ((q.data?.length ?? 0) === 0) return <EmptyState message={t('noData')} />
  return <InvoiceTable rows={q.data ?? []} onOpen={onOpenInvoice} />
}

function InvoiceTable({
  rows,
  onOpen,
}: {
  rows: JsonMap[]
  onOpen: (id: string) => void
}) {
  return (
    <div className="card" style={{ overflowX: 'auto' }}>
      <table className="list-table">
        <thead>
          <tr>
            <th>Invoice</th>
            <th>Date</th>
            <th>Customer</th>
            <th>Total</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => {
            const net = str(r, 'invoiceNetworkStatus', 'networkStatus', 'id')
            return (
              <tr key={net || String(i)}>
                <td>{str(r, 'invoiceNumber', 'invoiceNo')}</td>
                <td>{str(r, 'invoiceDate', 'date')}</td>
                <td>{str(r, 'customerName') || '—'}</td>
                <td>
                  <Money value={num(r, 'totalAmount', 'grandTotal')} />
                </td>
                <td>
                  {net ? (
                    <button type="button" className="btn btn-secondary" onClick={() => onOpen(net)}>
                      View
                    </button>
                  ) : (
                    <Link
                      to={`/reports?invoice=${encodeURIComponent(str(r, 'invoiceId'))}`}
                      className="btn btn-secondary"
                    >
                      View
                    </Link>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
