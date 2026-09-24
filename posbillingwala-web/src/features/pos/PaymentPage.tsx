import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Printer, Home } from 'lucide-react'
import { useAuthStore, useUserId, getOrCreateDeviceId } from '@/stores/authStore'
import { useCartStore } from '@/stores/cartStore'
import { useOnlineStore } from '@/stores/onlineStore'
import { useI18n } from '@/i18n'
import { PageHeader, Money, EmptyState } from '@/shared/ui'
import { saveInvoiceOnline } from './saveInvoice'

type PayMode = 'cash' | 'upi' | 'card' | 'split'

export function PaymentPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const online = useOnlineStore((s) => s.online)
  const userId = useUserId()
  const shopName = useAuthStore((s) => s.shopName)
  const staff = useAuthStore((s) => s.staff)
  const organizationId = useAuthStore((s) => s.organizationId)
  const branchId = useAuthStore((s) => s.branchId)

  const lines = useCartStore((s) => s.lines)
  const session = useCartStore((s) => s.session)
  const discount = useCartStore((s) => s.discount)
  const discountType = useCartStore((s) => s.discountType)
  const packingCharge = useCartStore((s) => s.packingCharge)
  const packingChargeType = useCartStore((s) => s.packingChargeType)
  const setDiscount = useCartStore((s) => s.setDiscount)
  const setPacking = useCartStore((s) => s.setPacking)
  const clearCart = useCartStore((s) => s.clearCart)
  const totals = useCartStore((s) => s.totals)

  const [mode, setMode] = useState<PayMode>('cash')
  const [cashAmount, setCashAmount] = useState('')
  const [upiAmount, setUpiAmount] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [receipt, setReceipt] = useState<{
    invoiceNumber: string
    invoiceNetworkStatus: string
    mode: PayMode
    totals: ReturnType<typeof totals>
    lines: typeof lines
    session: typeof session
  } | null>(null)

  const cartTotals = totals()

  const saveMut = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      if (!online) throw new Error(t('onlineRequired'))
      if (lines.length === 0) throw new Error(t('emptyCart'))

      const snapTotals = totals()
      let cash = 0
      let upi = 0
      if (mode === 'cash') cash = snapTotals.grandTotal
      else if (mode === 'upi') upi = snapTotals.grandTotal
      else if (mode === 'card') cash = 0
      else {
        cash = Number(cashAmount) || 0
        upi = Number(upiAmount) || 0
        if (Math.abs(cash + upi - snapTotals.grandTotal) > 0.05) {
          throw new Error('Split amounts must equal grand total')
        }
      }

      const result = await saveInvoiceOnline({
        userId,
        session,
        lines,
        subTotal: snapTotals.subTotal,
        gstTotal: snapTotals.gstTotal,
        discount,
        discountType,
        packingCharge,
        packingChargeType,
        totalAmount: snapTotals.grandTotal,
        paymentMode: mode,
        cashAmount: cash,
        upiAmount: upi,
        staffId: staff?.staffId,
        staffName: staff?.staffName,
        organizationId,
        branchId,
        deviceId: getOrCreateDeviceId(),
      })
      return {
        ...result,
        mode,
        totals: snapTotals,
        lines: [...lines],
        session: { ...session },
      }
    },
    onSuccess(result) {
      setReceipt(result)
      clearCart()
      setError(null)
    },
    onError(err) {
      setError(err instanceof Error ? err.message : 'Payment failed')
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    saveMut.mutate()
  }

  if (!online) {
    return (
      <div>
        <PageHeader title="Payment" />
        <EmptyState message={t('onlineRequired')} />
      </div>
    )
  }

  if (receipt) {
    return (
      <div>
        <PageHeader
          title="Payment successful"
          actions={
            <div className="row no-print">
              <button type="button" className="btn" onClick={() => window.print()}>
                <Printer size={16} /> {t('print')}
              </button>
              <Link to="/" className="btn btn-secondary">
                <Home size={16} /> {t('home')}
              </Link>
            </div>
          }
        />
        <div className="printable receipt">
          <h2>{shopName || 'Billingwala'}</h2>
          <h3>TAX INVOICE</h3>
          <div className="line">
            <span>Inv</span>
            <span>{receipt.invoiceNumber}</span>
          </div>
          <div className="line">
            <span>Type</span>
            <span>{receipt.session.invoiceType}</span>
          </div>
          {receipt.session.tableNumber ? (
            <div className="line">
              <span>Table</span>
              <span>{receipt.session.tableNumber}</span>
            </div>
          ) : null}
          {receipt.session.customerName ? (
            <div className="line">
              <span>Customer</span>
              <span>{receipt.session.customerName}</span>
            </div>
          ) : null}
          <hr />
          {receipt.lines.map((line) => (
            <div key={line.id} className="line">
              <span>
                {line.qty}× {line.name}
              </span>
              <span>
                <Money value={line.unitPrice * line.qty} />
              </span>
            </div>
          ))}
          <hr />
          <div className="line">
            <span>Subtotal</span>
            <span>
              <Money value={receipt.totals.subTotal} />
            </span>
          </div>
          <div className="line">
            <span>GST</span>
            <span>
              <Money value={receipt.totals.gstTotal} />
            </span>
          </div>
          <div className="line">
            <span>Discount</span>
            <span>
              <Money value={receipt.totals.discountAmount} />
            </span>
          </div>
          <div className="line">
            <span>Packing</span>
            <span>
              <Money value={receipt.totals.packingAmount} />
            </span>
          </div>
          <hr />
          <div className="line">
            <strong>TOTAL</strong>
            <strong>
              <Money value={receipt.totals.grandTotal} />
            </strong>
          </div>
          <div className="line">
            <span>Paid via</span>
            <span>{receipt.mode.toUpperCase()}</span>
          </div>
          <p style={{ textAlign: 'center', marginTop: '1rem' }}>Thank you!</p>
        </div>
        <p className="muted no-print" style={{ textAlign: 'center', marginTop: '1rem' }}>
          Ref: {receipt.invoiceNetworkStatus}
        </p>
        <div className="row no-print" style={{ justifyContent: 'center', marginTop: '1rem' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate(receipt.session.billingRoute)}
          >
            New bill
          </button>
        </div>
      </div>
    )
  }

  if (lines.length === 0) {
    return (
      <div>
        <PageHeader title="Payment" />
        <EmptyState message={t('emptyCart')} />
        <Link to={session.billingRoute} className="btn btn-secondary" style={{ marginTop: '1rem' }}>
          Back to billing
        </Link>
      </div>
    )
  }

  return (
    <div>
      <PageHeader title="Payment" subtitle={session.title} />
      <form className="card" style={{ maxWidth: 480 }} onSubmit={onSubmit}>
        <div className="row space-between" style={{ marginBottom: '0.75rem' }}>
          <span>Subtotal</span>
          <Money value={cartTotals.subTotal} />
        </div>
        <div className="row space-between" style={{ marginBottom: '0.75rem' }}>
          <span>GST</span>
          <Money value={cartTotals.gstTotal} />
        </div>

        <div className="field">
          <label htmlFor="discount">Discount ({discountType})</label>
          <div className="row">
            <input
              id="discount"
              type="number"
              min={0}
              step="0.01"
              value={discount}
              onChange={(e) => setDiscount(Number(e.target.value) || 0, discountType)}
            />
            <select
              value={discountType}
              onChange={(e) =>
                setDiscount(discount, e.target.value === 'percent' ? 'percent' : 'amount')
              }
            >
              <option value="amount">₹</option>
              <option value="percent">%</option>
            </select>
          </div>
        </div>

        <div className="field">
          <label htmlFor="packing">Packing ({packingChargeType})</label>
          <div className="row">
            <input
              id="packing"
              type="number"
              min={0}
              step="0.01"
              value={packingCharge}
              onChange={(e) =>
                setPacking(Number(e.target.value) || 0, packingChargeType)
              }
            />
            <select
              value={packingChargeType}
              onChange={(e) =>
                setPacking(
                  packingCharge,
                  e.target.value === 'percent' ? 'percent' : 'amount',
                )
              }
            >
              <option value="amount">₹</option>
              <option value="percent">%</option>
            </select>
          </div>
        </div>

        <div className="row space-between" style={{ marginBottom: '1rem' }}>
          <strong>Grand total</strong>
          <strong>
            <Money value={cartTotals.grandTotal} />
          </strong>
        </div>

        <div className="field">
          <label>Payment mode</label>
          <div className="chip-row">
            {(['cash', 'upi', 'card', 'split'] as const).map((m) => (
              <button
                key={m}
                type="button"
                className={`chip${mode === m ? ' active' : ''}`}
                onClick={() => setMode(m)}
              >
                {m.toUpperCase()}
              </button>
            ))}
          </div>
        </div>

        {mode === 'split' ? (
          <>
            <div className="field">
              <label htmlFor="cashAmt">Cash amount</label>
              <input
                id="cashAmt"
                type="number"
                min={0}
                step="0.01"
                value={cashAmount}
                onChange={(e) => setCashAmount(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="upiAmt">UPI amount</label>
              <input
                id="upiAmt"
                type="number"
                min={0}
                step="0.01"
                value={upiAmount}
                onChange={(e) => setUpiAmount(e.target.value)}
              />
            </div>
          </>
        ) : null}

        {error ? <p className="error-text">{error}</p> : null}

        <button className="btn" type="submit" disabled={saveMut.isPending} style={{ width: '100%' }}>
          {saveMut.isPending ? t('loading') : t('pay')}
        </button>
      </form>
    </div>
  )
}
