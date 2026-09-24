import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTakeawayStore } from '@/stores/takeawayStore'
import { useCartStore } from '@/stores/cartStore'
import { useI18n } from '@/i18n'
import { PageHeader, EmptyState } from '@/shared/ui'

export function TakeawayPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const parcels = useTakeawayStore((s) => s.parcels)
  const addParcel = useTakeawayStore((s) => s.addParcel)
  const removeParcel = useTakeawayStore((s) => s.removeParcel)
  const setSession = useCartStore((s) => s.setSession)
  const clearCart = useCartStore((s) => s.clearCart)

  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')

  const openParcels = parcels.filter((p) => p.status === 'open')

  function onAdd(e: FormEvent) {
    e.preventDefault()
    addParcel(name, phone)
    setName('')
    setPhone('')
  }

  function startBilling(parcelId: string, customerName: string, customerPhone: string) {
    clearCart()
    setSession({
      invoiceType: 'takeaway',
      title: `Takeaway — ${customerName}`,
      billingRoute: '/takeaway/billing',
      paymentRoute: '/takeaway/payment',
      customerName,
      customerPhone,
      parcelId,
      cartScope: `takeaway-${parcelId}`,
    })
    navigate('/takeaway/billing')
  }

  return (
    <div>
      <PageHeader title={t('takeaway')} subtitle="Open parcels" />

      <form className="card" style={{ maxWidth: 420, marginBottom: '1.25rem' }} onSubmit={onAdd}>
        <div className="field">
          <label htmlFor="tw-name">Customer name</label>
          <input
            id="tw-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Walk-in"
          />
        </div>
        <div className="field">
          <label htmlFor="tw-phone">Phone</label>
          <input
            id="tw-phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            inputMode="tel"
          />
        </div>
        <button type="submit" className="btn">
          {t('add')} parcel
        </button>
      </form>

      {openParcels.length === 0 ? (
        <EmptyState message="No open parcels" />
      ) : (
        <div className="stack">
          {openParcels.map((p) => (
            <div key={p.id} className="card row space-between">
              <div>
                <strong>{p.customerName}</strong>
                <div className="muted">{p.customerPhone || '—'}</div>
                <div className="muted" style={{ fontSize: '0.8rem' }}>
                  {new Date(p.createdAt).toLocaleString()}
                </div>
              </div>
              <div className="row">
                <button
                  type="button"
                  className="btn"
                  onClick={() => startBilling(p.id, p.customerName, p.customerPhone)}
                >
                  Start billing
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => removeParcel(p.id)}
                >
                  {t('delete')}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
