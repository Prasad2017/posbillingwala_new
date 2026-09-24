import { create } from 'zustand'

export type InvoiceType = 'fast_billing' | 'dine_in' | 'takeaway'

export interface CartLine {
  id: string
  kind: 'product' | 'combo'
  productId?: string
  comboId?: string
  name: string
  portionId?: string
  portionName?: string
  unitPrice: number
  qty: number
  gstPercent: number
  note?: string
}

export interface BillingSession {
  invoiceType: InvoiceType
  title: string
  billingRoute: string
  paymentRoute: string
  customerName?: string
  customerPhone?: string
  customerEmail?: string
  customerAddress?: string
  tableNumber?: string
  diningSessionId?: string
  parcelId?: string
  cartScope: string
}

interface CartState {
  session: BillingSession
  lines: CartLine[]
  discount: number
  discountType: 'amount' | 'percent'
  packingCharge: number
  packingChargeType: 'amount' | 'percent'
  setSession: (session: Partial<BillingSession> & Pick<BillingSession, 'invoiceType' | 'title' | 'billingRoute' | 'paymentRoute'>) => void
  resetSession: (session?: BillingSession) => void
  addLine: (line: Omit<CartLine, 'id'> & { id?: string }) => void
  updateQty: (id: string, qty: number) => void
  removeLine: (id: string) => void
  clearCart: () => void
  setDiscount: (value: number, type?: 'amount' | 'percent') => void
  setPacking: (value: number, type?: 'amount' | 'percent') => void
  totals: () => {
    subTotal: number
    gstTotal: number
    discountAmount: number
    packingAmount: number
    grandTotal: number
  }
}

export const defaultFastBillingSession: BillingSession = {
  invoiceType: 'fast_billing',
  title: 'Fast Billing',
  billingRoute: '/pos',
  paymentRoute: '/pos/payment',
  cartScope: 'pos',
}

function lineKey(line: Omit<CartLine, 'id' | 'qty'>): string {
  return [
    line.kind,
    line.productId ?? '',
    line.comboId ?? '',
    line.portionId ?? '',
    line.unitPrice,
    line.note ?? '',
  ].join('|')
}

export const useCartStore = create<CartState>((set, get) => ({
  session: defaultFastBillingSession,
  lines: [],
  discount: 0,
  discountType: 'amount',
  packingCharge: 0,
  packingChargeType: 'amount',

  setSession(session) {
    set((state) => ({
      session: { ...state.session, ...session },
    }))
  },

  resetSession(session = defaultFastBillingSession) {
    set({
      session,
      lines: [],
      discount: 0,
      discountType: 'amount',
      packingCharge: 0,
      packingChargeType: 'amount',
    })
  },

  addLine(line) {
    set((state) => {
      const key = lineKey(line)
      const existing = state.lines.find((l) => lineKey(l) === key)
      if (existing) {
        return {
          lines: state.lines.map((l) =>
            l.id === existing.id ? { ...l, qty: l.qty + line.qty } : l,
          ),
        }
      }
      const id =
        line.id ??
        `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
      return { lines: [...state.lines, { ...line, id }] }
    })
  },

  updateQty(id, qty) {
    if (qty <= 0) {
      get().removeLine(id)
      return
    }
    set((state) => ({
      lines: state.lines.map((l) => (l.id === id ? { ...l, qty } : l)),
    }))
  },

  removeLine(id) {
    set((state) => ({ lines: state.lines.filter((l) => l.id !== id) }))
  },

  clearCart() {
    set({
      lines: [],
      discount: 0,
      packingCharge: 0,
    })
  },

  setDiscount(value, type = 'amount') {
    set({ discount: value, discountType: type })
  },

  setPacking(value, type = 'amount') {
    set({ packingCharge: value, packingChargeType: type })
  },

  totals() {
    const { lines, discount, discountType, packingCharge, packingChargeType } =
      get()
    const subTotal = lines.reduce((sum, l) => sum + l.unitPrice * l.qty, 0)
    const gstTotal = lines.reduce(
      (sum, l) => sum + (l.unitPrice * l.qty * l.gstPercent) / 100,
      0,
    )
    const discountAmount =
      discountType === 'percent' ? (subTotal * discount) / 100 : discount
    const packingAmount =
      packingChargeType === 'percent'
        ? (subTotal * packingCharge) / 100
        : packingCharge
    const grandTotal = Math.max(
      0,
      subTotal + gstTotal - discountAmount + packingAmount,
    )
    return { subTotal, gstTotal, discountAmount, packingAmount, grandTotal }
  },
}))
