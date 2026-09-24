import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { JsonMap } from '@/api/client'
import { isApiSuccess, apiMessage } from '@/api/client'
import { inventoryApi } from '@/api/services'
import { useUserId } from '@/stores/authStore'
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

type Tab = 'stock' | 'expenses'

export function InventoryPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [tab, setTab] = useState<Tab>('stock')
  const [msg, setMsg] = useState<string | null>(null)

  const [itemName, setItemName] = useState('')
  const [qty, setQty] = useState('')
  const [unit, setUnit] = useState('kg')

  const [expTitle, setExpTitle] = useState('')
  const [expAmount, setExpAmount] = useState('')
  const [expNote, setExpNote] = useState('')

  const stockQ = useQuery({
    queryKey: ['inventory-stock', userId],
    enabled: Boolean(userId) && tab === 'stock',
    queryFn: () => inventoryApi.getInventory(userId!),
  })

  const expQ = useQuery({
    queryKey: ['inventory-expenses', userId],
    enabled: Boolean(userId) && tab === 'expenses',
    queryFn: () => inventoryApi.getExpenses(userId!),
  })

  const addStock = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await inventoryApi.insertInventory({
        userId,
        itemName: itemName.trim(),
        quantity: qty,
        unit,
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setItemName('')
      setQty('')
      setMsg('Stock item added')
      void qc.invalidateQueries({ queryKey: ['inventory-stock', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const addExp = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await inventoryApi.insertExpense({
        userId,
        expenseTitle: expTitle.trim(),
        expenseAmount: expAmount,
        expenseNote: expNote.trim(),
        expenseDate: new Date().toISOString().slice(0, 10),
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setExpTitle('')
      setExpAmount('')
      setExpNote('')
      setMsg('Expense added')
      void qc.invalidateQueries({ queryKey: ['inventory-expenses', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  return (
    <div>
      <PageHeader title={t('inventory')} />

      <div className="tabs">
        <button
          type="button"
          className={`tab${tab === 'stock' ? ' active' : ''}`}
          onClick={() => {
            setTab('stock')
            setMsg(null)
          }}
        >
          Stock
        </button>
        <button
          type="button"
          className={`tab${tab === 'expenses' ? ' active' : ''}`}
          onClick={() => {
            setTab('expenses')
            setMsg(null)
          }}
        >
          Expenses
        </button>
      </div>

      {msg ? <p className="muted">{msg}</p> : null}

      {tab === 'stock' ? (
        <div className="stack">
          <form
            className="card"
            style={{ maxWidth: 420 }}
            onSubmit={(e: FormEvent) => {
              e.preventDefault()
              addStock.mutate()
            }}
          >
            <div className="field">
              <label htmlFor="item">Item name</label>
              <input
                id="item"
                value={itemName}
                onChange={(e) => setItemName(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="qty">Quantity</label>
              <input
                id="qty"
                type="number"
                min={0}
                step="0.01"
                value={qty}
                onChange={(e) => setQty(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="unit">Unit</label>
              <input id="unit" value={unit} onChange={(e) => setUnit(e.target.value)} />
            </div>
            <button type="submit" className="btn" disabled={addStock.isPending}>
              {t('add')}
            </button>
          </form>

          {stockQ.isLoading ? <LoadingBlock /> : null}
          {(stockQ.data?.length ?? 0) === 0 && !stockQ.isLoading ? (
            <EmptyState message={t('noData')} />
          ) : (
            <div className="card" style={{ overflowX: 'auto' }}>
              <table className="list-table">
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Qty</th>
                    <th>Unit</th>
                  </tr>
                </thead>
                <tbody>
                  {(stockQ.data ?? []).map((r, i) => (
                    <tr key={str(r, 'inventoryId', 'id') || String(i)}>
                      <td>{str(r, 'itemName', 'inventoryName', 'name')}</td>
                      <td>{str(r, 'quantity', 'qty')}</td>
                      <td>{str(r, 'unit')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        <div className="stack">
          <form
            className="card"
            style={{ maxWidth: 420 }}
            onSubmit={(e: FormEvent) => {
              e.preventDefault()
              addExp.mutate()
            }}
          >
            <div className="field">
              <label htmlFor="et">Title</label>
              <input
                id="et"
                value={expTitle}
                onChange={(e) => setExpTitle(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="ea">Amount</label>
              <input
                id="ea"
                type="number"
                min={0}
                step="0.01"
                value={expAmount}
                onChange={(e) => setExpAmount(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="en">Note</label>
              <input id="en" value={expNote} onChange={(e) => setExpNote(e.target.value)} />
            </div>
            <button type="submit" className="btn" disabled={addExp.isPending}>
              {t('add')}
            </button>
          </form>

          {expQ.isLoading ? <LoadingBlock /> : null}
          {(expQ.data?.length ?? 0) === 0 && !expQ.isLoading ? (
            <EmptyState message={t('noData')} />
          ) : (
            <div className="card" style={{ overflowX: 'auto' }}>
              <table className="list-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Amount</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {(expQ.data ?? []).map((r, i) => (
                    <tr key={str(r, 'expenseId', 'id') || String(i)}>
                      <td>{str(r, 'expenseTitle', 'title', 'name')}</td>
                      <td>
                        <Money value={num(r, 'expenseAmount', 'amount')} />
                      </td>
                      <td>{str(r, 'expenseDate', 'date')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
