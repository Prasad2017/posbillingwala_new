import { apiGet } from './api'

export async function getWebsiteSettings() {
  const data = await apiGet('/settings')
  return data.settings || {}
}

export async function getPageContent(slug) {
  const data = await apiGet(`/pages/${slug}`)
  return data.page || null
}

export async function getTestimonials() {
  const data = await apiGet('/testimonials')
  return data.testimonials || []
}

export async function getCustomers() {
  const data = await apiGet('/clients')
  return data.clients || []
}
