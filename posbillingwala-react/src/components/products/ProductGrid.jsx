import { useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import ProductCard from './ProductCard'
import { PRODUCT_CATEGORIES } from '../../utils/constants'
import './ProductGrid.css'

export default function ProductGrid({ products = [] }) {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return products.filter((p) => {
      const catOk = !category || p.category === category
      if (!catOk) return false
      if (!q) return true
      return (
        String(p.name || '').toLowerCase().includes(q) ||
        String(p.description || '').toLowerCase().includes(q)
      )
    })
  }, [products, query, category])

  return (
    <div className="product-grid-wrap">
      <div className="product-toolbar">
        <label className="product-search">
          <Search size={18} />
          <input
            type="search"
            placeholder="Search products…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </label>
        <div className="product-filters" role="tablist" aria-label="Product categories">
          {PRODUCT_CATEGORIES.map((c) => (
            <button
              key={c.id || 'all'}
              type="button"
              className={category === c.id ? 'is-active' : ''}
              onClick={() => setCategory(c.id)}
            >
              {c.label}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <p className="product-empty">No products found. Try changing your search.</p>
      ) : (
        <div className="product-grid">
          {filtered.map((product, i) => (
            <ProductCard key={product.id} product={product} delay={Math.min(i * 0.04, 0.3)} />
          ))}
        </div>
      )}
    </div>
  )
}
