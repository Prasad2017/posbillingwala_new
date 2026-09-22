import { apiGet } from './api'

export async function getPricing() {
  const data = await apiGet('/pricing')
  return data.plans || []
}
