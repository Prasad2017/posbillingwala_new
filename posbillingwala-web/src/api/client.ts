import axios, { type AxiosInstance, type AxiosError } from 'axios'
import { API_BASE_URL } from './endpoints'

export type JsonMap = Record<string, unknown>

export function asJsonMap(data: unknown): JsonMap {
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    return data as JsonMap
  }
  return {}
}

export function isApiSuccess(data: JsonMap): boolean {
  const status = String(data.status ?? '')
  return status === '1' || status === 'success' || status === 'true'
}

export function apiMessage(data: JsonMap, fallback = 'Request failed'): string {
  const msg = data.message ?? data.msg ?? data.error
  return msg != null && String(msg).trim() ? String(msg) : fallback
}

export function listFrom(
  data: JsonMap,
  key: string,
): JsonMap[] {
  const raw = data[key]
  if (Array.isArray(raw)) return raw as JsonMap[]
  return []
}

type TokenGetter = () => string | null
type StaffIdGetter = () => string | null
type RefreshFn = () => Promise<string | null>
type SoftLogoutFn = () => void

let getToken: TokenGetter = () => null
let getStaffId: StaffIdGetter = () => null
let refreshToken: RefreshFn = async () => null
let softLogout: SoftLogoutFn = () => undefined

export function bindAuthHandlers(handlers: {
  getToken: TokenGetter
  getStaffId: StaffIdGetter
  refreshToken: RefreshFn
  softLogout: SoftLogoutFn
}) {
  getToken = handlers.getToken
  getStaffId = handlers.getStaffId
  refreshToken = handlers.refreshToken
  softLogout = handlers.softLogout
}

function ensureTrailingSlash(url: string): string {
  return url.endsWith('/') ? url : `${url}/`
}

export const api: AxiosInstance = axios.create({
  baseURL: ensureTrailingSlash(API_BASE_URL),
  timeout: 45000,
  headers: {
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const staffId = getStaffId()
  if (staffId) {
    config.headers['X-Pos-Staff-Id'] = staffId
  }
  return config
})

let refreshing: Promise<string | null> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config
    if (!original || error.response?.status !== 401) {
      return Promise.reject(error)
    }
    const alreadyRetried = Boolean((original as { _retry?: boolean })._retry)
    if (alreadyRetried) {
      softLogout()
      return Promise.reject(error)
    }
    ;(original as { _retry?: boolean })._retry = true
    refreshing ??= refreshToken().finally(() => {
      refreshing = null
    })
    const token = await refreshing
    if (!token) {
      softLogout()
      return Promise.reject(error)
    }
    original.headers.Authorization = `Bearer ${token}`
    return api.request(original)
  },
)

export async function postForm(
  path: string,
  fields: Record<string, string | number | boolean | null | undefined> = {},
): Promise<JsonMap> {
  const body = new URLSearchParams()
  for (const [key, value] of Object.entries(fields)) {
    if (value === undefined || value === null) continue
    body.append(key, String(value))
  }
  const response = await api.post(path, body, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
  return asJsonMap(response.data)
}

export async function getQuery(
  path: string,
  params: Record<string, string | number | boolean | null | undefined> = {},
): Promise<JsonMap> {
  const clean: Record<string, string> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) continue
    clean[key] = String(value)
  }
  const response = await api.get(path, { params: clean })
  return asJsonMap(response.data)
}
