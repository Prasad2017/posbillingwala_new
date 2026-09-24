import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { JsonMap } from '@/api/client'
import { isApiSuccess, apiMessage } from '@/api/client'
import { messApi } from '@/api/services'
import { useUserId } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { PageHeader, LoadingBlock, ErrorBlock, EmptyState } from '@/shared/ui'

function str(row: JsonMap, ...keys: string[]): string {
  for (const k of keys) {
    const v = row[k]
    if (v != null && String(v).trim()) return String(v)
  }
  return ''
}

type Tab = 'members' | 'tokens' | 'qr'

export function MessPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [tab, setTab] = useState<Tab>('members')

  const [memberName, setMemberName] = useState('')
  const [memberPhone, setMemberPhone] = useState('')
  const [memberCode, setMemberCode] = useState('')

  const [tokenName, setTokenName] = useState('')
  const [tokenPhone, setTokenPhone] = useState('')
  const [verifyCode, setVerifyCode] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const membersQ = useQuery({
    queryKey: ['mess-members', userId],
    enabled: Boolean(userId) && tab === 'members',
    queryFn: () => messApi.getMembers(userId!),
  })

  const tokensQ = useQuery({
    queryKey: ['mess-tokens', userId],
    enabled: Boolean(userId) && tab === 'tokens',
    queryFn: () => messApi.getTokens(userId!),
  })

  const qrQ = useQuery({
    queryKey: ['mess-qr', userId],
    enabled: Boolean(userId) && tab === 'qr',
    queryFn: () => messApi.getCommonQr(userId!),
  })

  const addMember = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.insertMember({
        userId,
        memberName: memberName.trim(),
        memberMobile: memberPhone.trim(),
        memberCode: memberCode.trim(),
        memberStatus: '1',
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setMemberName('')
      setMemberPhone('')
      setMemberCode('')
      setMsg('Member added')
      void qc.invalidateQueries({ queryKey: ['mess-members', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const createToken = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.insertToken({
        userId,
        guestName: tokenName.trim() || 'Walk-in',
        guestMobile: tokenPhone.trim(),
        tokenType: 'walkin',
        mealType: 'lunch',
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setTokenName('')
      setTokenPhone('')
      setMsg('Token created')
      void qc.invalidateQueries({ queryKey: ['mess-tokens', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const verifyToken = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.verifyToken({
        userId,
        tokenCode: verifyCode.trim(),
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setVerifyCode('')
      setMsg('Token verified')
      void qc.invalidateQueries({ queryKey: ['mess-tokens', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Verify failed')
    },
  })

  const genQr = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await messApi.generateCommonQr(userId)
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setMsg('QR generated')
      void qc.invalidateQueries({ queryKey: ['mess-qr', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'QR failed')
    },
  })

  function onAddMember(e: FormEvent) {
    e.preventDefault()
    addMember.mutate()
  }

  const qrPayload = qrQ.data
  const qrCode =
    qrPayload &&
    (str(qrPayload, 'qrCode', 'commonQr', 'qr', 'code') ||
      str((qrPayload.data as JsonMap) ?? {}, 'qrCode', 'commonQr'))

  return (
    <div>
      <PageHeader title={t('mess')} />

      <div className="row" style={{ marginBottom: '1rem', flexWrap: 'wrap' }}>
        <Link to="/mess/meal-sessions" className="btn btn-secondary">
          Meal sessions
        </Link>
        <Link to="/mess/payments" className="btn btn-secondary">
          Payments
        </Link>
        <Link to="/mess/scan" className="btn btn-secondary">
          Scan
        </Link>
        <Link to="/mess/meal-tokens-today" className="btn btn-secondary">
          Today&apos;s tokens
        </Link>
      </div>

      <div className="tabs">
        {([
          ['members', 'Members'],
          ['tokens', 'Tokens'],
          ['qr', 'Common QR'],
        ] as const).map(([id, label]) => (
          <button
            key={id}
            type="button"
            className={`tab${tab === id ? ' active' : ''}`}
            onClick={() => {
              setTab(id)
              setMsg(null)
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {msg ? <p className="muted">{msg}</p> : null}

      {tab === 'members' ? (
        <div className="stack">
          <form className="card" style={{ maxWidth: 420 }} onSubmit={onAddMember}>
            <div className="field">
              <label htmlFor="m-name">Name</label>
              <input
                id="m-name"
                value={memberName}
                onChange={(e) => setMemberName(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="m-phone">Mobile</label>
              <input
                id="m-phone"
                value={memberPhone}
                onChange={(e) => setMemberPhone(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="m-code">Member code</label>
              <input
                id="m-code"
                value={memberCode}
                onChange={(e) => setMemberCode(e.target.value)}
              />
            </div>
            <button type="submit" className="btn" disabled={addMember.isPending}>
              {t('add')} member
            </button>
          </form>

          {membersQ.isLoading ? <LoadingBlock /> : null}
          {membersQ.isError ? <ErrorBlock message="Failed to load members" /> : null}
          {(membersQ.data?.length ?? 0) === 0 && !membersQ.isLoading ? (
            <EmptyState message={t('noData')} />
          ) : (
            <div className="card" style={{ overflowX: 'auto' }}>
              <table className="list-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Mobile</th>
                    <th>Code</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {(membersQ.data ?? []).map((m, i) => (
                    <tr key={str(m, 'memberId', 'id') || String(i)}>
                      <td>{str(m, 'memberName', 'name')}</td>
                      <td>{str(m, 'memberMobile', 'mobile')}</td>
                      <td>{str(m, 'memberCode', 'code')}</td>
                      <td>{str(m, 'memberStatus', 'status') || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : null}

      {tab === 'tokens' ? (
        <div className="stack">
          <form
            className="card"
            style={{ maxWidth: 420 }}
            onSubmit={(e) => {
              e.preventDefault()
              createToken.mutate()
            }}
          >
            <h3 style={{ marginTop: 0 }}>Walk-in token</h3>
            <div className="field">
              <label htmlFor="t-name">Guest name</label>
              <input
                id="t-name"
                value={tokenName}
                onChange={(e) => setTokenName(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="t-phone">Mobile</label>
              <input
                id="t-phone"
                value={tokenPhone}
                onChange={(e) => setTokenPhone(e.target.value)}
              />
            </div>
            <button type="submit" className="btn" disabled={createToken.isPending}>
              Create token
            </button>
          </form>

          <form
            className="card"
            style={{ maxWidth: 420 }}
            onSubmit={(e) => {
              e.preventDefault()
              verifyToken.mutate()
            }}
          >
            <h3 style={{ marginTop: 0 }}>Verify token</h3>
            <div className="field">
              <label htmlFor="v-code">Token code</label>
              <input
                id="v-code"
                value={verifyCode}
                onChange={(e) => setVerifyCode(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn" disabled={verifyToken.isPending}>
              {t('verify')}
            </button>
          </form>

          {tokensQ.isLoading ? <LoadingBlock /> : null}
          {(tokensQ.data?.length ?? 0) === 0 && !tokensQ.isLoading ? (
            <EmptyState message={t('noData')} />
          ) : (
            <div className="card" style={{ overflowX: 'auto' }}>
              <table className="list-table">
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Guest</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {(tokensQ.data ?? []).map((tok, i) => (
                    <tr key={str(tok, 'tokenId', 'id') || String(i)}>
                      <td>{str(tok, 'tokenCode', 'code')}</td>
                      <td>{str(tok, 'guestName', 'memberName', 'name')}</td>
                      <td>{str(tok, 'tokenStatus', 'status') || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : null}

      {tab === 'qr' ? (
        <div className="stack" style={{ maxWidth: 480 }}>
          {qrQ.isLoading ? <LoadingBlock /> : null}
          {qrQ.isError ? <ErrorBlock message="Failed to load QR" /> : null}
          <div className="card">
            <p className="muted">Common mess QR</p>
            {qrCode ? (
              <pre
                style={{
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                  background: '#f6f8fb',
                  padding: '0.75rem',
                  borderRadius: 8,
                }}
              >
                {qrCode}
              </pre>
            ) : (
              <EmptyState message="No QR yet" />
            )}
            <button
              type="button"
              className="btn"
              style={{ marginTop: '0.75rem' }}
              disabled={genQr.isPending}
              onClick={() => genQr.mutate()}
            >
              Generate QR
            </button>
          </div>
          {qrPayload ? (
            <details className="card">
              <summary>Raw response</summary>
              <pre style={{ fontSize: 11, overflow: 'auto' }}>
                {JSON.stringify(qrPayload, null, 2)}
              </pre>
            </details>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
