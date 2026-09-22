import { apiGet } from './api'

export async function getProducts() {
  const data = await apiGet('/products')
  return data.products || []
}

export async function getProductById(id) {
  const products = await getProducts()
  return products.find((p) => String(p.id) === String(id)) || null
}
