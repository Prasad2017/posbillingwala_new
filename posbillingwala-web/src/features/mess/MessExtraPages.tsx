import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { JsonMap } from '@/api/client'
import { isApiSuccess, apiMessage, listFrom } from '@/api/client'
import { messApi } from '@/api/services'
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

function asList(data: JsonMap, ...keys: string[]): JsonMap[] {
  for (const k of keys) {
    const list = listFrom(data, k)
    if (list.length) return list
  }
  if (Array.isArray(data)) return data as JsonMap[]
  return []
}

export function MealSessionsPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [mealType, setMealType] = useState('lunch')
  const [msg, setMsg] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['mess-meal-sessions', userId],
    enabled: Boolean(userId),
    queryFn: () => messApi.getMealSessions(userId!),
  })

  const save = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.saveMealSession({
        userId,
        mealType,
        sessionDate: new Date().toISOString().slice(0, 10),
        status: 'open',
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setMsg('Session saved')
      void qc.invalidateQueries({ queryKey: ['mess-meal-sessions', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const rows = q.data ? asList(q.data, 'mealSessionResponse', 'sessions', 'data') : []

  return (
    <div>
      <PageHeader
        title="Meal sessions"
        actions={
          <Link to="/mess" className="btn btn-secondary">
            Back
          </Link>
        }
      />
      {msg ? <p className="muted">{msg}</p> : null}
      <form
        className="card row"
        style={{ maxWidth: 480, marginBottom: '1rem' }}
        onSubmit={(e) => {
          e.preventDefault()
          save.mutate()
        }}
      >
        <div className="field" style={{ flex: 1, marginBottom: 0 }}>
          <label htmlFor="meal">Meal type</label>
          <select id="meal" value={mealType} onChange={(e) => setMealType(e.target.value)}>
            <option value="breakfast">Breakfast</option>
            <option value="lunch">Lunch</option>
            <option value="dinner">Dinner</option>
          </select>
        </div>
        <button type="submit" className="btn" disabled={save.isPending}>
          {t('save')}
        </button>
      </form>
      {q.isLoading ? <LoadingBlock /> : null}
      {rows.length === 0 && !q.isLoading ? (
        <EmptyState message={t('noData')} />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Meal</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={str(r, 'id', 'sessionId') || String(i)}>
                  <td>{str(r, 'sessionDate', 'date')}</td>
                  <td>{str(r, 'mealType', 'meal')}</td>
                  <td>{str(r, 'status', 'sessionStatus')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export function MessPaymentsPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [memberId, setMemberId] = useState('')
  const [amount, setAmount] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['mess-payments', userId],
    enabled: Boolean(userId),
    queryFn: () => messApi.getPayments(userId!),
  })

  const save = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.insertPayment({
        userId,
        memberId,
        amount: amount,
        paymentMode: 'cash',
        paymentDate: new Date().toISOString().slice(0, 10),
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setAmount('')
      setMsg('Payment saved')
      void qc.invalidateQueries({ queryKey: ['mess-payments', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  return (
    <div>
      <PageHeader
        title="Mess payments"
        actions={
          <Link to="/mess" className="btn btn-secondary">
            Back
          </Link>
        }
      />
      {msg ? <p className="muted">{msg}</p> : null}
      <form
        className="card"
        style={{ maxWidth: 420, marginBottom: '1rem' }}
        onSubmit={(e: FormEvent) => {
          e.preventDefault()
          save.mutate()
        }}
      >
        <div className="field">
          <label htmlFor="mid">Member ID</label>
          <input
            id="mid"
            value={memberId}
            onChange={(e) => setMemberId(e.target.value)}
            required
          />
        </div>
        <div className="field">
          <label htmlFor="amt">Amount</label>
          <input
            id="amt"
            type="number"
            min={0}
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="btn" disabled={save.isPending}>
          {t('save')}
        </button>
      </form>
      {q.isLoading ? <LoadingBlock /> : null}
      {(q.data?.length ?? 0) === 0 && !q.isLoading ? (
        <EmptyState message={t('noData')} />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                <th>Member</th>
                <th>Amount</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {(q.data ?? []).map((p, i) => (
                <tr key={str(p, 'paymentId', 'id') || String(i)}>
                  <td>{str(p, 'memberName', 'memberId')}</td>
                  <td>
                    <Money value={num(p, 'amount', 'paymentAmount')} />
                  </td>
                  <td>{str(p, 'paymentDate', 'date')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export function MessScanPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const [code, setCode] = useState('')
  const [result, setResult] = useState<string | null>(null)

  const verify = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.verifyToken({
        userId,
        tokenCode: code.trim(),
        scanSource: 'web',
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess(res) {
      setResult(apiMessage(res, 'Verified'))
      setCode('')
    },
    onError(err) {
      setResult(err instanceof Error ? err.message : 'Scan failed')
    },
  })

  return (
    <div>
      <PageHeader
        title="Scan token"
        actions={
          <Link to="/mess" className="btn btn-secondary">
            Back
          </Link>
        }
      />
      <form
        className="card"
        style={{ maxWidth: 420 }}
        onSubmit={(e) => {
          e.preventDefault()
          verify.mutate()
        }}
      >
        <div className="field">
          <label htmlFor="scan">Token / QR code</label>
          <input
            id="scan"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            autoFocus
            required
          />
        </div>
        {result ? <p className="muted">{result}</p> : null}
        <button type="submit" className="btn" disabled={verify.isPending}>
          {t('verify')}
        </button>
      </form>
    </div>
  )
}

export function MessTokensTodayPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()

  const q = useQuery({
    queryKey: ['mess-tokens-today', userId],
    enabled: Boolean(userId),
    queryFn: () => messApi.getTodayTokens(userId!),
  })

  const rows = q.data
    ? asList(q.data, 'messTokenResponse', 'tokens', 'mealTokenResponse', 'data')
    : []

  return (
    <div>
      <PageHeader
        title="Today's meal tokens"
        actions={
          <Link to="/mess" className="btn btn-secondary">
            Back
          </Link>
        }
      />
      {q.isLoading ? <LoadingBlock /> : null}
      {rows.length === 0 && !q.isLoading ? (
        <EmptyState message={t('noData')} />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Meal</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={str(r, 'tokenId', 'id') || String(i)}>
                  <td>{str(r, 'tokenCode', 'code')}</td>
                  <td>{str(r, 'guestName', 'memberName', 'name')}</td>
                  <td>{str(r, 'mealType', 'meal')}</td>
                  <td>{str(r, 'tokenStatus', 'status')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
