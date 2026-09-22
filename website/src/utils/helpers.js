export function formatPrice(value) {
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0) return 'Contact for price'
  return `₹${n.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

export function formatPhoneDisplay(raw) {
  const digits = String(raw || '').replace(/\D/g, '')
  if (digits.length === 10) {
    return `+91 ${digits.slice(0, 5)} ${digits.slice(5)}`
  }
  if (digits.length === 12 && digits.startsWith('91')) {
    return `+91 ${digits.slice(2, 7)} ${digits.slice(7)}`
  }
  return raw || ''
}

export function digitsOnly(value) {
  return String(value || '').replace(/\D/g, '')
}

export function whatsappLink(number, message = '') {
  const digits = digitsOnly(number)
  const withCountry = digits.length === 10 ? `91${digits}` : digits
  const base = `https://wa.me/${withCountry}`
  if (!message) return base
  return `${base}?text=${encodeURIComponent(message)}`
}

export function telLink(number) {
  const digits = digitsOnly(number)
  return `tel:${digits}`
}

export function splitFeatures(description) {
  if (!description) return []
  return String(description)
    .split(/\n|•/)
    .map((s) => s.trim())
    .filter(Boolean)
}

export function formatDate(iso) {
  if (!iso) return ''
  try {
    return new Date(iso).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return ''
  }
}

export function mapsEmbedUrl(address) {
  if (!address) return ''
  return `https://maps.google.com/maps?q=${encodeURIComponent(address)}&output=embed`
}

export function safeExternalUrl(url) {
  if (!url) return null
  try {
    const u = new URL(url, window.location.origin)
    if (u.protocol === 'http:' || u.protocol === 'https:') return u.href
  } catch {
    return null
  }
  return null
}
