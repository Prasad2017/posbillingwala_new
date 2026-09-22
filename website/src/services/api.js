import axios from 'axios'

const PROD_ADMIN = 'https://admin.posbillingwala.com/api/website'
const LOCAL_ADMIN = 'http://127.0.0.1:8000/api/website'

function isLocalHost() {
  if (typeof window === 'undefined') return false
  const host = (window.location.hostname || '').toLowerCase()
  return host === 'localhost' || host === '127.0.0.1' || host.endsWith('.local')
}

function sameOriginApi() {
  if (typeof window === 'undefined') return ''
  try {
    if (window.location?.origin && String(window.location.protocol).includes('http')) {
      return `${window.location.origin.replace(/\/$/, '')}/api/website`
    }
  } catch {
    /* ignore */
  }
  return ''
}

export function getApiCandidates() {
  const envBase = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
  if (envBase) return [envBase]

  if (isLocalHost()) {
    return [LOCAL_ADMIN, 'http://localhost:8000/api/website', PROD_ADMIN]
  }

  const same = sameOriginApi()
  return same ? [same, PROD_ADMIN] : [PROD_ADMIN]
}

let resolvedBase = ''

export function getResolvedBase() {
  return resolvedBase
}

export function setResolvedBase(base) {
  resolvedBase = String(base || '').replace(/\/$/, '')
}

/** Recover JSON when upstream prepends binary junk (host/malware corruption). */
function bytesToLatin1(bytes) {
  let out = ''
  for (let i = 0; i < bytes.length; i += 1) out += String.fromCharCode(bytes[i])
  return out
}

function parseApiPayload(raw) {
  if (raw && typeof raw === 'object' && !ArrayBuffer.isView(raw) && !(raw instanceof ArrayBuffer)) {
    // Already-parsed object (or unexpected shape)
    if (raw.success !== undefined || raw.message !== undefined || raw.errors !== undefined) {
      return raw
    }
  }

  let text = ''
  if (typeof raw === 'string') {
    text = raw
  } else if (raw instanceof ArrayBuffer) {
    text = bytesToLatin1(new Uint8Array(raw))
  } else if (ArrayBuffer.isView(raw)) {
    text = bytesToLatin1(new Uint8Array(raw.buffer, raw.byteOffset, raw.byteLength))
  } else {
    return null
  }

  const trimmed = text.replace(/^\uFEFF/, '').trim()
  if (!trimmed) return null

  try {
    return JSON.parse(trimmed)
  } catch {
    /* continue */
  }

  const marker = trimmed.indexOf('{"success"')
  const start = marker >= 0 ? marker : trimmed.indexOf('{')
  if (start < 0) return null

  try {
    return JSON.parse(trimmed.slice(start))
  } catch {
    return null
  }
}

const client = axios.create({
  timeout: 20000,
  headers: { Accept: 'application/json' },
  // ArrayBuffer keeps leading binary junk intact so we can slice to JSON.
  responseType: 'arraybuffer',
  transformResponse: [(data) => data],
})

export async function apiGet(path) {
  const candidates = resolvedBase ? [resolvedBase] : getApiCandidates()
  let lastError

  for (const base of candidates) {
    try {
      const { data: raw, status } = await client.get(`${base}${path}`)
      const data = parseApiPayload(raw)
      if (status >= 400 || !data || data.success !== true) {
        throw new Error(
          (data && data.message) || `${base} invalid response`,
        )
      }
      setResolvedBase(base)
      return data
    } catch (err) {
      lastError = err
    }
  }

  const message =
    lastError?.response?.data?.message ||
    lastError?.message ||
    'Could not reach Admin API'
  const error = new Error(typeof message === 'string' ? message : 'Could not reach Admin API')
  error.cause = lastError
  throw error
}

export async function apiPost(path, body) {
  const candidates = resolvedBase ? [resolvedBase] : getApiCandidates()
  let lastError

  for (const base of candidates) {
    try {
      const { data: raw, status } = await client.post(`${base}${path}`, body, {
        headers: { 'Content-Type': 'application/json' },
      })
      const data = parseApiPayload(raw)
      if (status >= 400 || !data || data.success !== true) {
        const msg = data?.message || Object.values(data?.errors || {})?.[0]?.[0] || 'Request failed'
        throw new Error(msg)
      }
      setResolvedBase(base)
      return data
    } catch (err) {
      lastError = err
      if (err?.response?.status >= 400 && err?.response?.status < 500) {
        const data = parseApiPayload(err.response.data)
        const msg =
          data?.message ||
          Object.values(data?.errors || {})?.[0]?.[0] ||
          err.message
        throw new Error(msg)
      }
    }
  }

  throw new Error(
    lastError?.response?.data?.message ||
      lastError?.message ||
      'Could not reach Admin API',
  )
}

export default client
