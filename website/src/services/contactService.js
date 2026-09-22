import { apiPost } from './api'

export async function submitContact({ name, email, subject = '', message }) {
  return apiPost('/contact', { name, email, subject, message })
}
