import { SITE_NAME, SITE_TAGLINE } from './constants'

const SITE_URL = (import.meta.env.VITE_SITE_URL || 'https://posbillingwala.com').replace(/\/$/, '')

export function buildSeo({
  title,
  description = SITE_TAGLINE,
  path = '/',
  image,
} = {}) {
  const fullTitle = title ? `${title} | ${SITE_NAME}` : `${SITE_NAME} — ${SITE_TAGLINE}`
  const url = `${SITE_URL}${path.startsWith('/') ? path : `/${path}`}`
  return {
    title: fullTitle,
    description,
    canonical: url,
    ogTitle: fullTitle,
    ogDescription: description,
    ogUrl: url,
    ogImage: image || `${SITE_URL}/favicon.png`,
  }
}
