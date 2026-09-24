import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Printer } from 'lucide-react'
import type { JsonMap } from '@/api/client'
import { invoiceApi } from '@/api/services'
import { useAuthStore, useUserId } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { PageHeader, LoadingBlock, EmptyState, Money, ErrorBlock } from '@/shared/ui'

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

export function InvoiceDetailPage() {
  const { invoiceId = '' } = useParams()
  const networkStatus = decodeURIComponent(invoiceId)
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const shopName = useAuthStore((s) => s.shopName)

  const q = useQuery({
    queryKey: ['invoice-products', userId, networkStatus],
    enabled: Boolean(userId) && Boolean(networkStatus),
    queryFn: () => invoiceApi.getInvoiceProducts(userId!, networkStatus),
  })

  const lines = q.data ?? []
  const subTotal = lines.reduce(
    (sum, l) => sum + num(l, 'amount', 'lineAmount', 'productAmount'),
    0,
  )
  const gstTotal = lines.reduce((sum, l) => {
    const amt = num(l, 'amount', 'lineAmount')
    const gst = num(l, 'gstPercent', 'gst')
    return sum + (amt * gst) / 100
  }, 0)

  return (
    <div>
      <PageHeader
        title="Invoice"
        subtitle={networkStatus}
        actions={
          <div className="row no-print">
            <button type="button" className="btn" onClick={() => window.print()}>
              <Printer size={16} /> {t('print')}
            </button>
            <Link to="/reports" className="btn btn-secondary">
              Back
            </Link>
          </div>
        }
      />

      {q.isLoading ? <LoadingBlock /> : null}
      {q.isError ? <ErrorBlock message="Failed to load invoice lines" /> : null}
      {!q.isLoading && lines.length === 0 ? (
        <EmptyState message="No products on this invoice" />
      ) : null}

      {lines.length > 0 ? (
        <div className="printable receipt">
          <h2>{shopName || 'Billingwala'}</h2>
          <h3>TAX INVOICE</h3>
          <div className="line">
            <span>Ref</span>
            <span>{networkStatus}</span>
          </div>
          <hr />
          {lines.map((l, i) => (
            <div key={str(l, 'invoiceProductId', 'id') || String(i)} className="line">
              <span>
                {str(l, 'quantity', 'qty') || '1'}×{' '}
                {str(l, 'productName', 'comboName', 'name')}
                {str(l, 'portionName') ? ` (${str(l, 'portionName')})` : ''}
              </span>
              <span>
                <Money value={num(l, 'amount', 'lineAmount')} />
              </span>
            </div>
          ))}
          <hr />
          <div className="line">
            <span>Subtotal</span>
            <span>
              <Money value={subTotal} />
            </span>
          </div>
          <div className="line">
            <span>GST (est.)</span>
            <span>
              <Money value={gstTotal} />
            </span>
          </div>
          <div className="line">
            <strong>TOTAL</strong>
            <strong>
              <Money value={subTotal + gstTotal} />
            </strong>
          </div>
          <p style={{ textAlign: 'center', marginTop: '1rem' }}>Thank you!</p>
        </div>
      ) : null}
    </div>
  )
}
