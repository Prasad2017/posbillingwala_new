import { Link } from 'react-router-dom'
import { Printer } from 'lucide-react'
import { useCartStore } from '@/stores/cartStore'
import { useAuthStore } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { PageHeader, EmptyState } from '@/shared/ui'

export function KotPreviewPage() {
  const t = useI18n((s) => s.t)
  const shopName = useAuthStore((s) => s.shopName)
  const lines = useCartStore((s) => s.lines)
  const session = useCartStore((s) => s.session)

  if (lines.length === 0) {
    return (
      <div>
        <PageHeader title="KOT Preview" />
        <EmptyState message={t('emptyCart')} />
        <Link to={session.billingRoute} className="btn btn-secondary" style={{ marginTop: '1rem' }}>
          Back to billing
        </Link>
      </div>
    )
  }

  return (
    <div>
      <PageHeader
        title="KOT Preview"
        actions={
          <div className="row no-print">
            <button type="button" className="btn" onClick={() => window.print()}>
              <Printer size={16} /> {t('print')}
            </button>
            <Link to={session.billingRoute} className="btn btn-secondary">
              Back
            </Link>
          </div>
        }
      />

      <div className="printable receipt">
        <h2>{shopName || 'Billingwala'}</h2>
        <h3>KITCHEN ORDER</h3>
        <div className="line">
          <span>Type</span>
          <span>{session.invoiceType}</span>
        </div>
        {session.tableNumber ? (
          <div className="line">
            <span>Table</span>
            <span>{session.tableNumber}</span>
          </div>
        ) : null}
        {session.customerName ? (
          <div className="line">
            <span>Customer</span>
            <span>{session.customerName}</span>
          </div>
        ) : null}
        <div className="line">
          <span>Time</span>
          <span>{new Date().toLocaleTimeString()}</span>
        </div>
        <hr />
        {lines.map((line) => (
          <div key={line.id} className="line" style={{ marginBottom: '0.35rem' }}>
            <span>
              {line.qty}× {line.name}
              {line.portionName ? ` (${line.portionName})` : ''}
              {line.note ? ` — ${line.note}` : ''}
            </span>
          </div>
        ))}
        <hr />
        <p style={{ textAlign: 'center' }}>--- END KOT ---</p>
      </div>
    </div>
  )
}
