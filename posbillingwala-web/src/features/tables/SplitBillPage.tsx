import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useCartStore } from '@/stores/cartStore'
import { useI18n } from '@/i18n'
import { PageHeader, Money, EmptyState } from '@/shared/ui'

type SplitMode = 'equal' | 'amount'

export function SplitBillPage() {
  const t = useI18n((s) => s.t)
  const lines = useCartStore((s) => s.lines)
  const session = useCartStore((s) => s.session)
  const totals = useCartStore((s) => s.totals)
  const cartTotals = totals()

  const [mode, setMode] = useState<SplitMode>('equal')
  const [parts, setParts] = useState(2)
  const [amountsText, setAmountsText] = useState('')

  const equalShares = useMemo(() => {
    const n = Math.max(1, parts)
    const each = cartTotals.grandTotal / n
    return Array.from({ length: n }, (_, i) => ({
      label: `Guest ${i + 1}`,
      amount: each,
    }))
  }, [parts, cartTotals.grandTotal])

  const amountShares = useMemo(() => {
    const amounts = amountsText
      .split(/[,\n]/)
      .map((x) => Number(x.trim()))
      .filter((n) => !Number.isNaN(n) && n > 0)
    const sum = amounts.reduce((a, b) => a + b, 0)
    return {
      shares: amounts.map((amount, i) => ({
        label: `Share ${i + 1}`,
        amount,
      })),
      sum,
      remaining: cartTotals.grandTotal - sum,
    }
  }, [amountsText, cartTotals.grandTotal])

  if (lines.length === 0) {
    return (
      <div>
        <PageHeader title="Split bill" />
        <EmptyState message={t('emptyCart')} />
        <Link to={session.billingRoute || '/tables'} className="btn btn-secondary" style={{ marginTop: '1rem' }}>
          Back
        </Link>
      </div>
    )
  }

  return (
    <div>
      <PageHeader
        title="Split bill"
        subtitle={session.title}
        actions={
          <Link to={session.paymentRoute || '/tables/payment'} className="btn">
            Continue to pay
          </Link>
        }
      />

      <div className="card" style={{ maxWidth: 480, marginBottom: '1rem' }}>
        <div className="row space-between">
          <strong>Bill total</strong>
          <strong>
            <Money value={cartTotals.grandTotal} />
          </strong>
        </div>
      </div>

      <div className="tabs">
        <button
          type="button"
          className={`tab${mode === 'equal' ? ' active' : ''}`}
          onClick={() => setMode('equal')}
        >
          Equal split
        </button>
        <button
          type="button"
          className={`tab${mode === 'amount' ? ' active' : ''}`}
          onClick={() => setMode('amount')}
        >
          By amount
        </button>
      </div>

      {mode === 'equal' ? (
        <div className="card stack" style={{ maxWidth: 480 }}>
          <div className="field">
            <label htmlFor="parts">Number of guests</label>
            <input
              id="parts"
              type="number"
              min={1}
              max={20}
              value={parts}
              onChange={(e) => setParts(Math.max(1, Number(e.target.value) || 1))}
            />
          </div>
          {equalShares.map((s) => (
            <div key={s.label} className="row space-between">
              <span>{s.label}</span>
              <Money value={s.amount} />
            </div>
          ))}
        </div>
      ) : (
        <div className="card stack" style={{ maxWidth: 480 }}>
          <div className="field">
            <label htmlFor="amounts">Amounts (comma or new line)</label>
            <textarea
              id="amounts"
              rows={4}
              value={amountsText}
              onChange={(e) => setAmountsText(e.target.value)}
              placeholder="100, 150, 200"
            />
          </div>
          {amountShares.shares.map((s) => (
            <div key={s.label} className="row space-between">
              <span>{s.label}</span>
              <Money value={s.amount} />
            </div>
          ))}
          <div className="row space-between">
            <span className="muted">Entered</span>
            <Money value={amountShares.sum} />
          </div>
          <div className="row space-between">
            <span className={Math.abs(amountShares.remaining) < 0.05 ? 'muted' : 'error-text'}>
              Remaining
            </span>
            <Money value={amountShares.remaining} />
          </div>
        </div>
      )}
    </div>
  )
}
