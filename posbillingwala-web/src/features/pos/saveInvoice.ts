import { isApiSuccess, apiMessage } from '@/api/client'
import { invoiceApi } from '@/api/services'
import type { CartLine } from '@/stores/cartStore'
import type { BillingSession } from '@/stores/cartStore'

function formatInvoiceDate(d = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function newNetworkStatus(): string {
  return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function newInvoiceNumber(prefix = 'PB'): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${prefix}${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

export interface SaveInvoiceInput {
  userId: string
  session: BillingSession
  lines: CartLine[]
  subTotal: number
  gstTotal: number
  discount: number
  discountType: string
  packingCharge: number
  packingChargeType: string
  totalAmount: number
  paymentMode: string
  cashAmount: number
  upiAmount: number
  staffId?: string | null
  staffName?: string | null
  organizationId?: string | null
  branchId?: string | null
  deviceId: string
}

export async function saveInvoiceOnline(input: SaveInvoiceInput) {
  const invoiceNetworkStatus = newNetworkStatus()
  const invoiceNumber = newInvoiceNumber(
    input.session.invoiceType === 'takeaway'
      ? 'TW'
      : input.session.invoiceType === 'dine_in'
        ? 'DI'
        : 'PB',
  )

  const invoiceRes = await invoiceApi.insertInvoice({
    userId: input.userId,
    noOfTable: input.session.tableNumber || '0',
    invoiceNumber,
    customerName: input.session.customerName || '',
    customerMobile: input.session.customerPhone || '',
    customerEmail: input.session.customerEmail || '',
    customerAddress: input.session.customerAddress || '',
    subTotal: input.subTotal.toFixed(2),
    totalGSTAmount: input.gstTotal.toFixed(2),
    discount: input.discount.toFixed(2),
    discountType: input.discountType,
    packingCharge: input.packingCharge.toFixed(2),
    packingChargeType: input.packingChargeType,
    totalAmount: input.totalAmount.toFixed(2),
    paymentMode: input.paymentMode,
    cashAmount: input.cashAmount.toFixed(2),
    upiAmount: input.upiAmount.toFixed(2),
    diningSessionId: input.session.diningSessionId || '',
    billPrintStatus: '1',
    invoiceDate: formatInvoiceDate(),
    invoiceType: input.session.invoiceType,
    invoiceOrderStatus: 'completed',
    invoiceNetworkStatus,
    organizationId: input.organizationId || '',
    branchId: input.branchId || '',
    deviceId: input.deviceId,
    createdByStaffId: input.staffId || '',
    createdByStaffName: input.staffName || '',
  })

  if (!isApiSuccess(invoiceRes)) {
    throw new Error(apiMessage(invoiceRes, 'Failed to save invoice'))
  }

  for (const line of input.lines) {
    if (line.kind === 'combo') {
      const res = await invoiceApi.insertInvoiceComboItem({
        userId: input.userId,
        invoiceNetworkStatus,
        comboId: line.comboId || '',
        comboName: line.name,
        quantity: line.qty,
        rate: line.unitPrice.toFixed(2),
        amount: (line.unitPrice * line.qty).toFixed(2),
        gstPercent: line.gstPercent,
      })
      if (!isApiSuccess(res)) {
        throw new Error(apiMessage(res, 'Failed to save combo line'))
      }
    } else {
      const res = await invoiceApi.insertInvoiceProduct({
        userId: input.userId,
        invoiceNetworkStatus,
        productId: line.productId || '',
        productName: line.name,
        portionId: line.portionId || '',
        portionName: line.portionName || '',
        quantity: line.qty,
        rate: line.unitPrice.toFixed(2),
        amount: (line.unitPrice * line.qty).toFixed(2),
        gstPercent: line.gstPercent,
      })
      if (!isApiSuccess(res)) {
        throw new Error(apiMessage(res, 'Failed to save product line'))
      }
    }
  }

  return { invoiceNumber, invoiceNetworkStatus }
}
