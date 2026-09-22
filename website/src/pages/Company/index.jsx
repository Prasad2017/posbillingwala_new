import { motion } from 'framer-motion'
import CountUp from 'react-countup'
import { MonitorSmartphone, Package, Store, Headset } from 'lucide-react'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import AnimatedCard from '../../components/common/AnimatedCard'
import Loader from '../../components/common/Loader'
import ErrorState from '../../components/common/ErrorState'
import { useApi } from '../../hooks/useApi'
import { getPageContent, getCustomers } from '../../services/websiteService'
import { getDealers } from '../../services/dealerService'
import { getProducts } from '../../services/productService'
import { useSettings } from '../../context/SettingsContext'
import { CMS_SLUGS } from '../../utils/constants'
import { formatDate } from '../../utils/helpers'
import servicesImage from '../../assets/images/business/services-image.jpg'
import '../pages.css'
import './Company.css'

const MODEL = [
  {
    icon: MonitorSmartphone,
    title: 'Software',
    text: 'Android POS for fast billing, inventory and reports.',
  },
  {
    icon: Package,
    title: 'Hardware & Products',
    text: 'Printers, rolls and accessories for the counter.',
  },
  {
    icon: Store,
    title: 'Authorized Dealers',
    text: 'Local partners for sales, setup and renewals.',
  },
  {
    icon: Headset,
    title: 'Customer Support',
    text: 'Help with installation, printers and daily use.',
  },
]

export default function Company() {
  const { companyName } = useSettings()
  const pageApi = useApi(() => getPageContent(CMS_SLUGS.company), [CMS_SLUGS.company])
  const clientsApi = useApi(getCustomers, [])
  const dealersApi = useApi(getDealers, [])
  const productsApi = useApi(getProducts, [])

  const stats = [
    { label: 'Businesses Featured', value: clientsApi.data?.length || 0 },
    { label: 'Dealer Locations', value: dealersApi.data?.length || 0 },
    { label: 'Products Listed', value: productsApi.data?.length || 0 },
  ].filter((s) => s.value > 0)

  return (
    <>
      <Seo
        title="Our Company"
        description="A practical business model built around software, products, dealers and customer support."
        path="/company"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">COMPANY MODEL</span>
          <h1>{pageApi.data?.title || 'Our Company'}</h1>
          <p className="page-hero-lead">
            A practical business model built around software, products, dealers and customer support.
          </p>
        </div>
      </section>

      <section className="section">
        <div className="container page-grid-2 company-who">
          <motion.div
            initial={{ opacity: 0, x: -24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <span className="eyebrow">Who we are</span>
            <h2>Building reliable POS for local businesses</h2>
            <p>
              {companyName} operates POS Billingwala as a complete solution — software, products and
              a dealer network that helps businesses bill faster with trusted local support.
            </p>
            <ul className="check-list">
              <li>Software designed for Indian counters</li>
              <li>Hardware and consumables in one place</li>
              <li>Dealer-led onboarding and renewals</li>
              <li>Ongoing customer support pathways</li>
            </ul>
          </motion.div>
          <motion.div
            className="split-visual"
            initial={{ opacity: 0, x: 24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <img src={servicesImage} alt="POS Billingwala company" loading="lazy" />
          </motion.div>
        </div>
      </section>

      {stats.length > 0 && (
        <section className="section section--muted">
          <div className="container">
            <div className="company-stats">
              {stats.map((item) => (
                <div key={item.label} className="company-stat">
                  <strong>
                    <CountUp end={item.value} duration={1.4} suffix="+" />
                  </strong>
                  <span>{item.label}</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className={`section${stats.length > 0 ? '' : ' section--muted'}`}>
        <div className="container">
          <SectionTitle
            eyebrow="How we work"
            title="A connected business model"
            subtitle="Software, products, dealers and support — working together."
          />
          <div className="page-grid-4 company-model">
            {MODEL.map((m, i) => (
              <AnimatedCard key={m.title} className="feature-tile company-model__card" delay={i * 0.05}>
                <div className="feature-tile__icon">
                  <m.icon size={22} />
                </div>
                <h3>{m.title}</h3>
                <p>{m.text}</p>
              </AnimatedCard>
            ))}
          </div>
        </div>
      </section>

      <section className="section section--muted">
        <div className="container">
          <article className="cms-card">
            {pageApi.loading && <Loader label="Loading content…" />}
            {pageApi.error && (
              <ErrorState message={pageApi.error} onRetry={pageApi.refetch} />
            )}
            {!pageApi.loading && !pageApi.error && pageApi.data && (
              <div
                className="legal-body"
                dangerouslySetInnerHTML={{ __html: pageApi.data.body_html || '' }}
              />
            )}
            {!pageApi.loading && !pageApi.error && !pageApi.data && (
              <p className="text-muted">
                Company details will appear here soon.
              </p>
            )}
            {pageApi.data?.updated_at && (
              <p className="cms-updated">Last Updated: {formatDate(pageApi.data.updated_at)}</p>
            )}
            <div className="cms-card__actions">
              <Button to="/contact" variant="primary">
                Contact Us
              </Button>
              <Button to="/dealers" variant="ghost">
                Become a Dealer
              </Button>
            </div>
          </article>
        </div>
      </section>
    </>
  )
}
