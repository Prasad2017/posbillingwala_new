import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '@/api/services'
import { useAuthStore, getOrCreateDeviceId } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { useOnlineStore } from '@/stores/onlineStore'

export function LoginPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const online = useOnlineStore((s) => s.online)
  const applyLoginPayload = useAuthStore((s) => s.applyLoginPayload)
  const setPhase = useAuthStore((s) => s.setPhase)
  const [licenceKey, setLicenceKey] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!online) {
      setError(t('onlineRequired'))
      return
    }
    setBusy(true)
    setError(null)
    try {
      const deviceId = getOrCreateDeviceId()
      const result = await authApi.loginCheck(licenceKey.trim(), deviceId)
      if (result.status === '3') {
        setError(result.message || 'Licence already active on another device.')
        return
      }
      if (result.status === '0') {
        setError(result.message || 'Login failed')
        return
      }
      applyLoginPayload(
        { ...result, licenceKey: result.licenceKey || licenceKey.trim() },
        { keepPhase: 'needsMpin' },
      )
      setPhase('needsMpin')
      navigate('/mpin')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-layout">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>{t('appName')}</h1>
        <p className="lead">Online browser POS — enter your shop licence key.</p>
        <div className="field">
          <label htmlFor="licence">{t('licenceKey')}</label>
          <input
            id="licence"
            value={licenceKey}
            onChange={(e) => setLicenceKey(e.target.value)}
            autoComplete="off"
            required
          />
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="btn" type="submit" disabled={busy || !licenceKey.trim()} style={{ width: '100%' }}>
          {busy ? t('loading') : t('login')}
        </button>
        <p className="muted" style={{ marginTop: '1rem', textAlign: 'center' }}>
          <Link to="/register">{t('registerTrial')}</Link>
        </p>
      </form>
    </div>
  )
}

export function MpinPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const online = useOnlineStore((s) => s.online)
  const licenceKey = useAuthStore((s) => s.licenceKey)
  const userManagementEnabled = useAuthStore((s) => s.userManagementEnabled)
  const applyLoginPayload = useAuthStore((s) => s.applyLoginPayload)
  const setPhase = useAuthStore((s) => s.setPhase)
  const setAuthToken = useAuthStore((s) => s.setAuthToken)
  const hardLogout = useAuthStore((s) => s.hardLogout)
  const [mpin, setMpin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!licenceKey) {
      navigate('/login')
      return
    }
    if (!online) {
      setError(t('onlineRequired'))
      return
    }
    setBusy(true)
    setError(null)
    try {
      const result = await authApi.loginMpin({
        mpin: mpin.trim(),
        licenceKey,
        deviceId: getOrCreateDeviceId(),
        deviceName: navigator.platform || 'Web POS',
      })
      if (result.status !== '1' || !result.authToken) {
        setError(result.message || 'Invalid PIN')
        return
      }
      applyLoginPayload(result)
      setAuthToken(result.authToken, result.tokenExpiresAt)
      if (userManagementEnabled || String(result.userManagementEnabled) === '1') {
        setPhase('needsStaff')
        navigate('/staff-login')
      } else {
        setPhase('authenticated')
        navigate('/')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unlock failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-layout">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>{t('mpin')}</h1>
        <p className="lead">Enter the 4-digit owner PIN to unlock this shop.</p>
        <div className="field">
          <label htmlFor="mpin">{t('mpin')}</label>
          <input
            id="mpin"
            type="password"
            inputMode="numeric"
            maxLength={6}
            value={mpin}
            onChange={(e) => setMpin(e.target.value.replace(/\D/g, ''))}
            required
          />
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="btn" type="submit" disabled={busy || mpin.length < 4} style={{ width: '100%' }}>
          {busy ? t('loading') : t('unlock')}
        </button>
        <p className="muted" style={{ marginTop: '1rem', textAlign: 'center' }}>
          <button
            type="button"
            className="btn-ghost"
            onClick={() => {
              hardLogout()
              navigate('/login')
            }}
          >
            Use another licence
          </button>
        </p>
      </form>
    </div>
  )
}

export function StaffLoginPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const online = useOnlineStore((s) => s.online)
  const licenceKey = useAuthStore((s) => s.licenceKey)
  const setStaff = useAuthStore((s) => s.setStaff)
  const setPhase = useAuthStore((s) => s.setPhase)
  const [mobile, setMobile] = useState('')
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!licenceKey) {
      navigate('/login')
      return
    }
    if (!online) {
      setError(t('onlineRequired'))
      return
    }
    setBusy(true)
    setError(null)
    try {
      const data = await authApi.staffLogin({
        licenceKey,
        mobileNumber: mobile.trim(),
        pin: pin.trim(),
        deviceId: getOrCreateDeviceId(),
      })
      const status = String(data.status ?? '')
      if (status !== '1') {
        setError(String(data.message ?? 'Staff login failed'))
        return
      }
      const permsRaw = data.permissions ?? data.permissionList ?? []
      const permissions = Array.isArray(permsRaw)
        ? permsRaw.map(String)
        : String(permsRaw)
            .split(',')
            .map((p) => p.trim())
            .filter(Boolean)
      setStaff({
        staffId: String(data.staffId ?? data.userId ?? data.id ?? ''),
        staffName: String(data.staffName ?? data.name ?? data.userName ?? 'Staff'),
        mobileNumber: mobile.trim(),
        role: data.role != null ? String(data.role) : undefined,
        permissions: permissions.length ? permissions : ['*'],
      })
      setPhase('authenticated')
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Staff login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-layout">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>{t('staffLogin')}</h1>
        <p className="lead">Sign in with your staff mobile and PIN.</p>
        <div className="field">
          <label htmlFor="mobile">Mobile</label>
          <input id="mobile" value={mobile} onChange={(e) => setMobile(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="pin">PIN</label>
          <input
            id="pin"
            type="password"
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            required
          />
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="btn" type="submit" disabled={busy} style={{ width: '100%' }}>
          {busy ? t('loading') : t('login')}
        </button>
      </form>
    </div>
  )
}

export function RegisterPage() {
  const t = useI18n((s) => s.t)
  const navigate = useNavigate()
  const online = useOnlineStore((s) => s.online)
  const [name, setName] = useState('')
  const [contact, setContact] = useState('')
  const [shopName, setShopName] = useState('')
  const [address, setAddress] = useState('')
  const [result, setResult] = useState<{
    licenceKey?: string
    mpin?: string
    reportPin?: string
    message: string
  } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const canSubmit = useMemo(
    () => name && contact && shopName && address && online,
    [name, contact, shopName, address, online],
  )

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const res = await authApi.registerTrial({
        name: name.trim(),
        contactNumber: contact.trim(),
        shopName: shopName.trim(),
        address: address.trim(),
      })
      if (!res.ok) {
        setError(res.message)
        return
      }
      setResult({
        licenceKey: res.licenceKey,
        mpin: res.mpin,
        reportPin: res.reportPin,
        message: res.message,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-layout">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>{t('registerTrial')}</h1>
        <p className="lead">Start a trial shop licence.</p>
        {result ? (
          <div className="stack">
            <p>{result.message}</p>
            {result.licenceKey ? <p><strong>Licence:</strong> {result.licenceKey}</p> : null}
            {result.mpin ? <p><strong>PB-PIN:</strong> {result.mpin}</p> : null}
            {result.reportPin ? <p><strong>Report PIN:</strong> {result.reportPin}</p> : null}
            <button type="button" className="btn" onClick={() => navigate('/login')}>
              Continue to login
            </button>
          </div>
        ) : (
          <>
            <div className="field">
              <label>Name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <div className="field">
              <label>Contact</label>
              <input value={contact} onChange={(e) => setContact(e.target.value)} required />
            </div>
            <div className="field">
              <label>Shop name</label>
              <input value={shopName} onChange={(e) => setShopName(e.target.value)} required />
            </div>
            <div className="field">
              <label>Address</label>
              <textarea value={address} onChange={(e) => setAddress(e.target.value)} required rows={3} />
            </div>
            {error ? <p className="error-text">{error}</p> : null}
            <button className="btn" type="submit" disabled={!canSubmit || busy} style={{ width: '100%' }}>
              {busy ? t('loading') : 'Create trial'}
            </button>
          </>
        )}
        <p className="muted" style={{ marginTop: '1rem', textAlign: 'center' }}>
          <Link to="/login">{t('login')}</Link>
        </p>
      </form>
    </div>
  )
}
