import Seo from '../../components/common/Seo'
import SectionTitle from '../../components/common/SectionTitle'
import ProductGrid from '../../components/products/ProductGrid'
import { CardSkeleton } from '../../components/common/Skeleton'
import ErrorState from '../../components/common/ErrorState'
import EmptyState from '../../components/common/EmptyState'
import Button from '../../components/common/Button'
import { useApi } from '../../hooks/useApi'
import { getProducts } from '../../services/productService'
import '../pages.css'
import './Products.css'

export default function Products() {
  const { data, loading, error, refetch } = useApi(getProducts, [])
  const products = data || []

  return (
    <>
      <Seo
        title="POS Products & Accessories"
        description="Browse POS software, printers, paper rolls and accessories for Indian businesses."
        path="/products"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">PRODUCTS</span>
          <h1>Everything Your Counter Needs</h1>
          <p className="page-hero-lead">
            Software, hardware and consumables — curated for restaurants, retail, grocery and
            growing businesses.
          </p>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Catalog"
            title="Shop our POS range"
            subtitle="Browse products by category and find the right setup."
          />

          {loading && (
            <div className="products-page__skeletons">
              {Array.from({ length: 8 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          )}

          {error && <ErrorState message={error} onRetry={refetch} />}

          {!loading && !error && products.length === 0 && (
            <EmptyState
              title="No products yet"
              message="Products will appear here soon."
            />
          )}

          {!loading && !error && products.length > 0 && <ProductGrid products={products} />}
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="cta-panel">
            <div>
              <h2>Need help choosing?</h2>
              <p>Tell us about your business and we’ll recommend the right setup.</p>
            </div>
            <div className="cta-panel__actions">
              <Button to="/contact?subject=Product%20Enquiry" variant="red" size="large">
                Contact Us
              </Button>
              <Button to="/dealers" variant="outline" size="large">
                Find a Dealer
              </Button>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
