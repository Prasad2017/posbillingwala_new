import { useMemo, useState } from 'react'
import { Star } from 'lucide-react'
import { Swiper, SwiperSlide } from 'swiper/react'
import { Autoplay } from 'swiper/modules'
import 'swiper/css'
import Seo from '../../components/common/Seo'
import SectionTitle from '../../components/common/SectionTitle'
import CustomerCard from '../../components/customers/CustomerCard'
import { CardSkeleton } from '../../components/common/Skeleton'
import ErrorState from '../../components/common/ErrorState'
import EmptyState from '../../components/common/EmptyState'
import Button from '../../components/common/Button'
import { useApi } from '../../hooks/useApi'
import { getCustomers, getTestimonials } from '../../services/websiteService'
import { BUSINESS_TYPES } from '../../utils/constants'
import '../pages.css'
import './Customers.css'

export default function Customers() {
  const clientsApi = useApi(getCustomers, [])
  const testimonialsApi = useApi(getTestimonials, [])
  const clients = clientsApi.data || []
  const [category, setCategory] = useState('')

  const categories = useMemo(() => {
    const fromData = new Set()
    clients.forEach((c) => {
      const cat = (c.business_category || '').trim().toLowerCase()
      if (cat) fromData.add(cat)
    })
    const known = BUSINESS_TYPES.map((b) => b.id)
    const tabs = [{ id: '', label: 'All' }]
    known.forEach((id) => {
      if (fromData.has(id)) {
        const match = BUSINESS_TYPES.find((b) => b.id === id)
        tabs.push({ id, label: match?.label || id })
      }
    })
    fromData.forEach((id) => {
      if (!known.includes(id)) {
        tabs.push({ id, label: id.charAt(0).toUpperCase() + id.slice(1) })
      }
    })
    return tabs
  }, [clients])

  const filtered = useMemo(() => {
    if (!category) return clients
    return clients.filter(
      (c) => String(c.business_category || '').trim().toLowerCase() === category,
    )
  }, [clients, category])

  const logos = useMemo(
    () => clients.filter((c) => c.logo_url).map((c) => ({ id: c.id, url: c.logo_url, name: c.business_name })),
    [clients],
  )

  const countLabel =
    clients.length > 0
      ? `Trusted by ${clients.length}${clients.length >= 10 ? '+' : ''} businesses`
      : 'Trusted by growing businesses'

  return (
    <>
      <Seo
        title="Our Customers"
        description="Restaurants, retail stores, grocery businesses and more using POS Billingwala."
        path="/customers"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">CUSTOMERS</span>
          <h1>{countLabel}</h1>
          <p className="page-hero-lead">
            Real businesses using POS Billingwala across restaurants, retail, grocery, mess and more.
          </p>
        </div>
      </section>

      {logos.length > 0 && (
        <section className="customers-marquee section" aria-label="Customer logos">
          <div className="customers-marquee__track">
            {[...logos, ...logos].map((logo, i) => (
              <div key={`${logo.id}-${i}`} className="customers-marquee__item">
                <img src={logo.url} alt={logo.name || 'Customer logo'} loading="lazy" />
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="section page-section--soft">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Customer stories"
            title="Businesses that bill with us"
            subtitle="Browse customer stories across restaurants, retail and more."
          />

          {!clientsApi.loading && !clientsApi.error && categories.length > 1 && (
            <div className="filter-tabs" role="tablist" aria-label="Business category">
              {categories.map((tab) => (
                <button
                  key={tab.id || 'all'}
                  type="button"
                  className={category === tab.id ? 'is-active' : ''}
                  onClick={() => setCategory(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          )}

          {clientsApi.loading && (
            <div className="page-cards">
              {Array.from({ length: 6 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          )}

          {clientsApi.error && (
            <ErrorState message={clientsApi.error} onRetry={clientsApi.refetch} />
          )}

          {!clientsApi.loading && !clientsApi.error && clients.length === 0 && (
            <EmptyState
              title="No customers published yet"
              message="Customer profiles will appear here soon."
            />
          )}

          {!clientsApi.loading && !clientsApi.error && clients.length > 0 && filtered.length === 0 && (
            <EmptyState title="No matches" message="Try another category." />
          )}

          {!clientsApi.loading && !clientsApi.error && filtered.length > 0 && (
            <div className="page-cards">
              {filtered.map((client, i) => (
                <CustomerCard key={client.id} client={client} delay={Math.min(i * 0.04, 0.28)} />
              ))}
            </div>
          )}
        </div>
      </section>

      {(testimonialsApi.loading ||
        (testimonialsApi.data && testimonialsApi.data.length > 0)) && (
        <section className="section page-section--lavender">
          <div className="container">
            <SectionTitle
              colorful
              eyebrow="Testimonials"
              title="What business owners say"
              subtitle="Real feedback from business owners using POS Billingwala."
            />
            {testimonialsApi.loading && <CardSkeleton />}
            {testimonialsApi.error && (
              <ErrorState message={testimonialsApi.error} onRetry={testimonialsApi.refetch} />
            )}
            {testimonialsApi.data?.length > 0 && (
              <Swiper
                modules={[Autoplay]}
                autoplay={{ delay: 4200, disableOnInteraction: false, pauseOnMouseEnter: true }}
                spaceBetween={18}
                slidesPerView={1}
                breakpoints={{ 768: { slidesPerView: 2 }, 1100: { slidesPerView: 3 } }}
              >
                {testimonialsApi.data.map((t) => (
                  <SwiperSlide key={t.id}>
                    <article className="customers-quote">
                      <div className="customers-quote__stars">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star
                            key={i}
                            size={16}
                            fill={i < (t.rating || 0) ? '#f59e0b' : 'transparent'}
                            color="#f59e0b"
                          />
                        ))}
                      </div>
                      <p>“{t.quote}”</p>
                      <strong>{t.author_name}</strong>
                      {t.business_name && <span>{t.business_name}</span>}
                    </article>
                  </SwiperSlide>
                ))}
              </Swiper>
            )}
          </div>
        </section>
      )}

      <section className="section">
        <div className="container">
          <div className="cta-panel">
            <div>
              <h2>Join businesses that bill smarter</h2>
              <p>Book a demo or download the app to get started.</p>
            </div>
            <div className="cta-panel__actions">
              <Button to="/contact?subject=Customer%20Demo" variant="red" size="large">
                Book Demo
              </Button>
              <Button to="/download-app" variant="outline" size="large">
                Download App
              </Button>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
