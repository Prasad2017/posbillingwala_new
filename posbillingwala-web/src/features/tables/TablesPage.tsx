import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Split } from 'lucide-react'
import type { JsonMap } from '@/api/client'
import { isApiSuccess, apiMessage } from '@/api/client'
import { tablesApi } from '@/api/services'
import { useUserId } from '@/stores/authStore'
import { useCartStore } from '@/stores/cartStore'
import { useI18n } from '@/i18n'
import { PageHeader, LoadingBlock, ErrorBlock, EmptyState } from '@/shared/ui'

function str(row: JsonMap, ...keys: string[]): string {
  for (const k of keys) {
    const v = row[k]
    if (v != null && String(v).trim()) return String(v)
  }
  return ''
}

function sessionCoversTable(session: JsonMap, tableNumber: string): boolean {
  const joined = str(session, 'joinedTableNumbers', 'tableNumbers', 'tableNumber')
  if (!joined) return false
  return joined
    .split(/[,|]/)
    .map((x) => x.trim())
    .filter(Boolean)
    .includes(tableNumber)
}

export function TablesPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const userId = useUserId()
  const setSession = useCartStore((s) => s.setSession)
  const clearCart = useCartStore((s) => s.clearCart)
  const qc = useQueryClient()
  const [busyTable, setBusyTable] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const data = useQuery({
    queryKey: ['tables', userId],
    enabled: Boolean(userId),
    queryFn: async () => {
      const id = userId!
      const [tables, sessions] = await Promise.all([
        tablesApi.getTables(id),
        tablesApi.getDiningSessions(id),
      ])
      return { tables, sessions }
    },
  })

  const occupiedByTable = useMemo(() => {
    const map = new Map<string, JsonMap>()
    for (const s of data.data?.sessions ?? []) {
      const status = str(s, 'sessionStatus', 'status').toUpperCase()
      if (['SETTLED', 'CLOSED', 'CANCELLED', 'PAID'].includes(status)) continue
      const joined = str(s, 'joinedTableNumbers', 'tableNumbers', 'tableNumber')
      for (const tn of joined.split(/[,|]/).map((x) => x.trim()).filter(Boolean)) {
        map.set(tn, s)
      }
    }
    return map
  }, [data.data?.sessions])

  const openMut = useMutation({
    mutationFn: async (table: JsonMap) => {
      if (!userId) throw new Error('Not signed in')
      const tableNumber = str(table, 'tableNumber')
      const existing = occupiedByTable.get(tableNumber)
      if (existing) {
        return {
          sessionId: str(existing, 'sessionId', 'diningSessionId', 'id'),
          tableNumber,
        }
      }
      const res = await tablesApi.insertDiningSession({
        userId,
        tableNumber,
        joinedTableNumbers: tableNumber,
        sessionStatus: 'RUNNING',
        covers: str(table, 'capacity') || '2',
        waiterName: '',
      })
      if (!isApiSuccess(res)) {
        throw new Error(apiMessage(res, 'Failed to open table'))
      }
      const sessionId =
        str(res, 'sessionId', 'diningSessionId', 'id') ||
        `local-${tableNumber}-${Date.now()}`
      return { sessionId, tableNumber }
    },
    onSuccess({ sessionId, tableNumber }) {
      clearCart()
      setSession({
        invoiceType: 'dine_in',
        title: `Table ${tableNumber}`,
        billingRoute: '/tables/billing',
        paymentRoute: '/tables/payment',
        tableNumber,
        diningSessionId: sessionId,
        cartScope: `table-${tableNumber}`,
      })
      void qc.invalidateQueries({ queryKey: ['tables', userId] })
      navigate('/tables/billing')
    },
    onError(err) {
      setError(err instanceof Error ? err.message : 'Failed to open table')
      setBusyTable(null)
    },
  })

  return (
    <div>
      <PageHeader
        title={t('tables')}
        actions={
          <Link to="/tables/split-bill" className="btn btn-secondary">
            <Split size={16} /> Split bill
          </Link>
        }
      />

      {data.isLoading ? <LoadingBlock /> : null}
      {data.isError ? <ErrorBlock message="Failed to load tables" /> : null}
      {error ? <p className="error-text">{error}</p> : null}

      {!data.isLoading && (data.data?.tables.length ?? 0) === 0 ? (
        <EmptyState message="No tables configured. Add them in Masters." />
      ) : null}

      <div className="table-grid">
        {(data.data?.tables ?? []).map((table) => {
          const tableNumber = str(table, 'tableNumber')
          const occupied = occupiedByTable.has(tableNumber)
          const session = occupiedByTable.get(tableNumber)
          return (
            <button
              key={str(table, 'tableId') || tableNumber}
              type="button"
              className={`table-tile${occupied ? ' occupied' : ''}`}
              disabled={busyTable === tableNumber || openMut.isPending}
              onClick={() => {
                setError(null)
                setBusyTable(tableNumber)
                openMut.mutate(table)
              }}
            >
              <strong>T{tableNumber}</strong>
              <div className="muted" style={{ marginTop: '0.35rem' }}>
                {str(table, 'tableName') || `Seats ${str(table, 'capacity') || '—'}`}
              </div>
              <div style={{ marginTop: '0.5rem', fontWeight: 600 }}>
                {occupied ? 'Occupied' : 'Vacant'}
              </div>
              {session && sessionCoversTable(session, tableNumber) ? (
                <div className="muted" style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>
                  {str(session, 'sessionStatus', 'status')}
                </div>
              ) : null}
            </button>
          )
        })}
      </div>
    </div>
  )
}
