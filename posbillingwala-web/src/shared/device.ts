const DEVICE_KEY = 'pb_web_device_id'
const DEVICE_NAME_KEY = 'pb_web_device_name'

function uuid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function getOrCreateDeviceId(): string {
  const existing = localStorage.getItem(DEVICE_KEY)
  if (existing) return existing
  const id = uuid()
  localStorage.setItem(DEVICE_KEY, id)
  return id
}

export function getDeviceName(): string {
  const existing = localStorage.getItem(DEVICE_NAME_KEY)
  if (existing) return existing
  const name = `Web POS (${navigator.platform || 'Browser'})`
  localStorage.setItem(DEVICE_NAME_KEY, name)
  return name
}
