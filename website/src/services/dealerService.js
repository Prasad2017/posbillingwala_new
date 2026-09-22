import { apiGet } from './api'

export async function getDealers() {
  const data = await apiGet('/dealers')
  return data.dealers || []
}
