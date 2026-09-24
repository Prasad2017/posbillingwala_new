import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { JsonMap } from '@/api/client'
import { isApiSuccess, apiMessage, listFrom } from '@/api/client'
import {
  companyApi,
  staffApi,
  printApi,
  deviceApi,
  supportApi,
} from '@/api/services'
import { useUserId, getOrCreateDeviceId, getDeviceName } from '@/stores/authStore'
import { useI18n, type Locale } from '@/i18n'
import { PageHeader, LoadingBlock, EmptyState } from '@/shared/ui'
import { createCloudPrintJob } from '@/features/print/printJob'

function str(row: JsonMap, ...keys: string[]): string {
  for (const k of keys) {
    const v = row[k]
    if (v != null && String(v).trim()) return String(v)
  }
  return ''
}

function asList(data: JsonMap | JsonMap[], ...keys: string[]): JsonMap[] {
  if (Array.isArray(data)) return data
  for (const k of keys) {
    const list = listFrom(data, k)
    if (list.length) return list
  }
  return []
}

type Hub =
  | 'menu'
  | 'company'
  | 'printers'
  | 'staff'
  | 'devices'
  | 'language'
  | 'support'
  | 'hours'

const HOURS_KEY = 'pb-business-hours'

export function SettingsPage() {
  const t = useI18n((s) => s.t)
  const [hub, setHub] = useState<Hub>('menu')

  if (hub === 'menu') {
    return (
      <div>
        <PageHeader title={t('settings')} />
        <div className="table-grid">
          {(
            [
              ['company', 'Company'],
              ['printers', 'Printers / Print jobs'],
              ['staff', 'Users / Staff'],
              ['devices', 'Devices'],
              ['language', 'Language'],
              ['support', 'Support'],
              ['hours', 'Business hours'],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              className="table-tile"
              onClick={() => setHub(id)}
            >
              <strong>{label}</strong>
            </button>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div>
      <PageHeader
        title={
          (
            {
              company: 'Company',
              printers: 'Printers',
              staff: 'Staff',
              devices: 'Devices',
              language: 'Language',
              support: 'Support',
              hours: 'Business hours',
              menu: t('settings'),
            } as const
          )[hub]
        }
        actions={
          <button type="button" className="btn btn-secondary" onClick={() => setHub('menu')}>
            Hub
          </button>
        }
      />
      {hub === 'company' ? <CompanySection /> : null}
      {hub === 'printers' ? <PrintersSection /> : null}
      {hub === 'staff' ? <StaffSection /> : null}
      {hub === 'devices' ? <DevicesSection /> : null}
      {hub === 'language' ? <LanguageSection /> : null}
      {hub === 'support' ? <SupportSection /> : null}
      {hub === 'hours' ? <BusinessHoursSection /> : null}
    </div>
  )
}

function CompanySection() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [shopName, setShopName] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [gstin, setGstin] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['company', userId],
    enabled: Boolean(userId),
    queryFn: () => companyApi.getCompanyList(userId!),
  })

  useEffect(() => {
    const first = q.data?.[0]
    if (!first) return
    setShopName(str(first, 'shopName', 'companyName', 'name'))
    setAddress(str(first, 'address', 'companyAddress'))
    setPhone(str(first, 'contactNumber', 'phone', 'mobile'))
    setGstin(str(first, 'gstin', 'gstNo'))
  }, [q.data])

  const save = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await companyApi.insertCompanyDetail({
        userId,
        shopName,
        address,
        contactNumber: phone,
        gstin,
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setMsg('Company saved')
      void qc.invalidateQueries({ queryKey: ['company', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  if (q.isLoading) return <LoadingBlock />

  return (
    <form
      className="card"
      style={{ maxWidth: 480 }}
      onSubmit={(e: FormEvent) => {
        e.preventDefault()
        save.mutate()
      }}
    >
      <div className="field">
        <label htmlFor="sn">Shop name</label>
        <input id="sn" value={shopName} onChange={(e) => setShopName(e.target.value)} required />
      </div>
      <div className="field">
        <label htmlFor="addr">Address</label>
        <textarea id="addr" rows={3} value={address} onChange={(e) => setAddress(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="ph">Phone</label>
        <input id="ph" value={phone} onChange={(e) => setPhone(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="gst">GSTIN</label>
        <input id="gst" value={gstin} onChange={(e) => setGstin(e.target.value)} />
      </div>
      {msg ? <p className="muted">{msg}</p> : null}
      <button type="submit" className="btn" disabled={save.isPending}>
        {t('save')}
      </button>
    </form>
  )
}

function PrintersSection() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [msg, setMsg] = useState<string | null>(null)

  const printersQ = useQuery({
    queryKey: ['store-printers', userId],
    enabled: Boolean(userId),
    queryFn: () => printApi.getStorePrinters(userId!),
  })

  const jobsQ = useQuery({
    queryKey: ['print-jobs', userId],
    enabled: Boolean(userId),
    queryFn: () => printApi.getJobs(userId!),
  })

  const testPrint = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      return createCloudPrintJob(userId, {
        documentType: 'TEST',
        documentId: `test-${Date.now()}`,
        text: 'Billingwala web POS test print',
      })
    },
    onSuccess() {
      setMsg('Test print job created')
      void qc.invalidateQueries({ queryKey: ['print-jobs', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const printers = printersQ.data
    ? asList(printersQ.data, 'storePrinterResponse', 'printers', 'data')
    : []
  const jobs = jobsQ.data
    ? asList(jobsQ.data, 'printJobResponse', 'jobs', 'data')
    : []

  return (
    <div className="stack">
      <div className="row">
        <button
          type="button"
          className="btn"
          disabled={testPrint.isPending}
          onClick={() => testPrint.mutate()}
        >
          Create test print job
        </button>
      </div>
      {msg ? <p className="muted">{msg}</p> : null}

      <h3 style={{ margin: 0 }}>Printers</h3>
      {printersQ.isLoading ? <LoadingBlock /> : null}
      {printers.length === 0 && !printersQ.isLoading ? (
        <EmptyState message="No store printers" />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Host</th>
              </tr>
            </thead>
            <tbody>
              {printers.map((p, i) => (
                <tr key={str(p, 'id', 'printerId') || String(i)}>
                  <td>{str(p, 'printerName', 'name')}</td>
                  <td>{str(p, 'connectionType', 'type')}</td>
                  <td>{str(p, 'ipAddress', 'host') || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h3 style={{ margin: 0 }}>Print jobs</h3>
      {jobsQ.isLoading ? <LoadingBlock /> : null}
      {jobs.length === 0 && !jobsQ.isLoading ? (
        <EmptyState message={t('noData')} />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((j, i) => (
                <tr key={str(j, 'id') || String(i)}>
                  <td>{str(j, 'id')}</td>
                  <td>{str(j, 'documentType', 'type')}</td>
                  <td>{str(j, 'status', 'jobStatus')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function StaffSection() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [name, setName] = useState('')
  const [mobile, setMobile] = useState('')
  const [pin, setPin] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['staff-list', userId],
    enabled: Boolean(userId),
    queryFn: () => staffApi.getList(userId!),
  })

  const add = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await staffApi.insert({
        userId,
        staffName: name.trim(),
        mobileNumber: mobile.trim(),
        appLoginPin: pin.trim(),
        role: 'cashier',
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setName('')
      setMobile('')
      setPin('')
      setMsg('Staff added')
      void qc.invalidateQueries({ queryKey: ['staff-list', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const rows = q.data
    ? asList(q.data, 'staffResponse', 'staff', 'users', 'data')
    : []

  return (
    <div className="stack">
      <form
        className="card"
        style={{ maxWidth: 420 }}
        onSubmit={(e: FormEvent) => {
          e.preventDefault()
          add.mutate()
        }}
      >
        <div className="field">
          <label htmlFor="stn">Name</label>
          <input id="stn" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="stm">Mobile</label>
          <input id="stm" value={mobile} onChange={(e) => setMobile(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="stp">App PIN</label>
          <input
            id="stp"
            type="password"
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            required
          />
        </div>
        {msg ? <p className="muted">{msg}</p> : null}
        <button type="submit" className="btn" disabled={add.isPending}>
          {t('add')}
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
                <th>Name</th>
                <th>Mobile</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={str(r, 'staffId', 'id') || String(i)}>
                  <td>{str(r, 'staffName', 'name')}</td>
                  <td>{str(r, 'mobileNumber', 'mobile')}</td>
                  <td>{str(r, 'role', 'staffRole') || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function DevicesSection() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [msg, setMsg] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['devices', userId],
    enabled: Boolean(userId),
    queryFn: () => deviceApi.getList(userId!),
  })

  const register = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await deviceApi.register({
        userId,
        android_device_id: getOrCreateDeviceId(),
        device_name: getDeviceName(),
        platform: 'web',
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setMsg('This browser registered as a device')
      void qc.invalidateQueries({ queryKey: ['devices', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  const rows = q.data
    ? asList(q.data, 'posDeviceResponse', 'devices', 'data')
    : []

  return (
    <div className="stack">
      <button
        type="button"
        className="btn"
        disabled={register.isPending}
        onClick={() => register.mutate()}
      >
        Register this browser
      </button>
      {msg ? <p className="muted">{msg}</p> : null}
      {q.isLoading ? <LoadingBlock /> : null}
      {rows.length === 0 && !q.isLoading ? (
        <EmptyState message={t('noData')} />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Device ID</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={str(r, 'id', 'deviceId') || String(i)}>
                  <td>{str(r, 'device_name', 'deviceName', 'name')}</td>
                  <td>{str(r, 'android_device_id', 'deviceId')}</td>
                  <td>{str(r, 'status') || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function LanguageSection() {
  const locale = useI18n((s) => s.locale)
  const setLocale = useI18n((s) => s.setLocale)
  const options: { id: Locale; label: string }[] = [
    { id: 'en', label: 'English' },
    { id: 'hi', label: 'हिन्दी' },
    { id: 'mr', label: 'मराठी' },
  ]

  return (
    <div className="card" style={{ maxWidth: 360 }}>
      <p className="muted">App language</p>
      <div className="chip-row">
        {options.map((o) => (
          <button
            key={o.id}
            type="button"
            className={`chip${locale === o.id ? ' active' : ''}`}
            onClick={() => setLocale(o.id)}
          >
            {o.label}
          </button>
        ))}
      </div>
    </div>
  )
}

function SupportSection() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [subject, setSubject] = useState('')
  const [body, setBody] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['support-tickets', userId],
    enabled: Boolean(userId),
    queryFn: () => supportApi.getTickets(userId!),
  })

  const create = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      const res = await supportApi.createTicket({
        userId,
        subject: subject.trim(),
        message: body.trim(),
      })
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setSubject('')
      setBody('')
      setMsg('Ticket created')
      void qc.invalidateQueries({ queryKey: ['support-tickets', userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Failed')
    },
  })

  return (
    <div className="stack">
      <form
        className="card"
        style={{ maxWidth: 480 }}
        onSubmit={(e: FormEvent) => {
          e.preventDefault()
          create.mutate()
        }}
      >
        <div className="field">
          <label htmlFor="sub">Subject</label>
          <input
            id="sub"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            required
          />
        </div>
        <div className="field">
          <label htmlFor="body">Message</label>
          <textarea
            id="body"
            rows={4}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            required
          />
        </div>
        {msg ? <p className="muted">{msg}</p> : null}
        <button type="submit" className="btn" disabled={create.isPending}>
          Create ticket
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
                <th>Subject</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {(q.data ?? []).map((r, i) => (
                <tr key={str(r, 'ticketId', 'id') || String(i)}>
                  <td>{str(r, 'subject', 'title')}</td>
                  <td>{str(r, 'status')}</td>
                  <td>{str(r, 'createdAt', 'date')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function BusinessHoursSection() {
  const t = useI18n((s) => s.t)
  const [open, setOpen] = useState('09:00')
  const [close, setClose] = useState('22:00')
  const [days, setDays] = useState('Mon–Sun')
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    try {
      const raw = localStorage.getItem(HOURS_KEY)
      if (!raw) return
      const parsed = JSON.parse(raw) as {
        open?: string
        close?: string
        days?: string
      }
      if (parsed.open) setOpen(parsed.open)
      if (parsed.close) setClose(parsed.close)
      if (parsed.days) setDays(parsed.days)
    } catch {
      /* ignore */
    }
  }, [])

  function onSave(e: FormEvent) {
    e.preventDefault()
    localStorage.setItem(HOURS_KEY, JSON.stringify({ open, close, days }))
    setSaved(true)
  }

  return (
    <form className="card" style={{ maxWidth: 420 }} onSubmit={onSave}>
      <p className="muted">Stored locally in this browser</p>
      <div className="field">
        <label htmlFor="days">Days</label>
        <input id="days" value={days} onChange={(e) => setDays(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="open">Opens</label>
        <input id="open" type="time" value={open} onChange={(e) => setOpen(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="close">Closes</label>
        <input id="close" type="time" value={close} onChange={(e) => setClose(e.target.value)} />
      </div>
      {saved ? <p className="muted">Saved</p> : null}
      <button type="submit" className="btn">
        {t('save')}
      </button>
    </form>
  )
}
