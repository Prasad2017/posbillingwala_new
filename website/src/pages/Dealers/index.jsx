import { useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import {
  IndianRupee,
  Megaphone,
  GraduationCap,
  Headset,
  Package,
  RefreshCw,
} from 'lucide-react'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import AnimatedCard from '../../components/common/AnimatedCard'
import IconBadge from '../../components/common/IconBadge'
import DealerSearch from '../../components/dealers/DealerSearch'
import DealerCard from '../../components/dealers/DealerCard'
import { CardSkeleton } from '../../components/common/Skeleton'
import ErrorState from '../../components/common/ErrorState'
import EmptyState from '../../components/common/EmptyState'
import { useApi } from '../../hooks/useApi'
import { getDealers } from '../../services/dealerService'
import servicesImage from '../../assets/images/business/services-image-04.jpg'
import partnerVisual from '../../assets/images/business/services-image-03.jpg'
import '../pages.css'
import './Dealers.css'

const BENEFITS = [
  {
    icon: IndianRupee,
    title: 'Attractive Profit Margin',
    text: 'Grow recurring software and hardware revenue.',
    tone: 'green',
  },
  {
    icon: Megaphone,
    title: 'Marketing Support',
    text: 'Get brand and product support for local sales.',
    tone: 'orange',
  },
  {
    icon: GraduationCap,
    title: 'Training & Guidance',
    text: 'Learn product setup and customer onboarding.',
    tone: 'purple',
  },
  {
    icon: Headset,
    title: 'Priority Support',
    text: 'Help for installations and troubleshooting.',
    tone: 'blue',
  },
  {
    icon: Package,
    title: 'Product Access',
    text: 'Software, printers and consumables.',
    tone: 'cyan',
  },
  {
    icon: RefreshCw,
    title: 'Customer Retention',
    text: 'Renewals and long-term support workflows.',
    tone: 'pink',
  },
]

export default function Dealers() {
  const { data, loading, error, refetch } = useApi(getDealers, [])
  const dealers = data || []
  const [query, setQuery] = useState('')
  const [area, setArea] = useState('')

  const areas = useMemo(() => {
    const set = new Set()
    dealers.forEach((d) => {
      if (d.area) set.add(String(d.area).trim())
    })
    return Array.from(set).sort((a, b) => a.localeCompare(b))
  }, [dealers])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return dealers.filter((d) => {
      if (area && String(d.area || '').trim() !== area) return false
      if (!q) return true
      const hay = [
        d.dealer_name,
        d.contact_person,
        d.area,
        d.address,
        d.mobile,
        d.whatsapp,
        d.role_title,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [dealers, query, area])

  return (
    <>
      <Seo
        title="Become Our Dealer Partner"
        description="Join the POS Billingwala dealer network. Sell software, printers and support local businesses."
        path="/dealers"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">DEALER NETWORK</span>
          <h1>Become Our Dealer Partner</h1>
          <p className="page-hero-lead">
            Build a local POS business with software, printers, training and ongoing customer
            support.
          </p>
          <div className="page-cta-bar">
            <Button to="/contact?subject=Dealer%20Application" variant="red" size="large">
              Apply Now
            </Button>
          </div>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container">
          <motion.article
            className="dealers-partner-card"
            initial={{ opacity: 0, y: 28 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
          >
            <div className="dealers-partner-card__copy">
              <span className="section-title__eyebrow section-title__eyebrow--badge">Partner with us</span>
              <h2 className="dealers-heading page-heading">
                Build a local POS business with strong margins
              </h2>
              <p className="dealers-lead">
                Become an authorized POS Billingwala dealer and help restaurants, shops and
                businesses nearby with software, printers, training and reliable support.
              </p>
              <div className="dealers-partner-card__points">
                <div className="dealers-partner-point dealers-partner-point--green">
                  <div className="dealers-partner-point__icon">
                    <IconBadge icon={IndianRupee} tone="green" size="lg" />
                  </div>
                  <strong>Earn on every sale</strong>
                  <span>Software licences, renewals and hardware kits</span>
                </div>
                <div className="dealers-partner-point dealers-partner-point--blue">
                  <div className="dealers-partner-point__icon">
                    <IconBadge icon={GraduationCap} tone="blue" size="lg" />
                  </div>
                  <strong>Get trained to sell</strong>
                  <span>Product demos, setup guidance and onboarding help</span>
                </div>
                <div className="dealers-partner-point dealers-partner-point--purple">
                  <div className="dealers-partner-point__icon">
                    <IconBadge icon={Headset} tone="purple" size="lg" />
                  </div>
                  <strong>Stay supported</strong>
                  <span>Priority assistance for your customers and installs</span>
                </div>
              </div>
              <div className="dealers-partner-card__actions">
                <Button to="/contact?subject=Dealer%20Application" variant="primary">
                  Become a Dealer
                </Button>
                <Button to="/contact?subject=Dealer%20Enquiry" variant="ghost">
                  Ask a Question
                </Button>
              </div>
            </div>

            <div className="dealers-partner-card__media">
              <div className="dealers-partner-card__glow" />
              <img
                className="dealers-partner-card__image"
                src={servicesImage}
                alt="Grow your POS dealer business with POS Billingwala"
                loading="lazy"
              />
              <img
                className="dealers-partner-card__float"
                src={partnerVisual}
                alt=""
                loading="lazy"
              />
              <div className="dealers-partner-card__chip">
                <IconBadge icon={Megaphone} tone="orange" size="sm" />
                <span>Marketing support included</span>
              </div>
            </div>
          </motion.article>
        </div>
      </section>

      <section className="section page-section--lavender">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Dealer benefits"
            title="Everything you need to serve customers"
            subtitle="Tools, training and support to grow a local POS practice."
          />
          <div className="page-grid-3">
            {BENEFITS.map((b, i) => (
              <AnimatedCard key={b.title} className={`feature-tile feature-tile--${b.tone}`} delay={i * 0.04}>
                <IconBadge icon={b.icon} tone={b.tone} />
                <h3>{b.title}</h3>
                <p>{b.text}</p>
              </AnimatedCard>
            ))}
          </div>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Find a dealer"
            title="Authorized dealers near you"
            subtitle="Search by area, name or phone."
          />

          {!loading && !error && dealers.length > 0 && (
            <DealerSearch
              query={query}
              onQueryChange={setQuery}
              areas={areas}
              area={area}
              onAreaChange={setArea}
            />
          )}

          {loading && (
            <div className="page-cards dealers-page__grid">
              {Array.from({ length: 6 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          )}

          {error && <ErrorState message={error} onRetry={refetch} />}

          {!loading && !error && dealers.length === 0 && (
            <EmptyState
              title="No dealers listed yet"
              message="Dealer partners will appear here soon."
            />
          )}

          {!loading && !error && dealers.length > 0 && filtered.length === 0 && (
            <EmptyState title="No dealers found" message="Try a different search or area." />
          )}

          {!loading && !error && filtered.length > 0 && (
            <div className="page-cards dealers-page__grid">
              {filtered.map((dealer, i) => (
                <DealerCard key={dealer.id} dealer={dealer} delay={Math.min(i * 0.04, 0.28)} />
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="cta-panel">
            <div>
              <h2>Ready to partner with us?</h2>
              <p>Send a dealer application and our team will get in touch.</p>
            </div>
            <div className="cta-panel__actions">
              <Button to="/contact?subject=Dealer%20Application" variant="red" size="large">
                Apply as Dealer
              </Button>
              <Button to="/contact" variant="outline" size="large">
                Talk to Us
              </Button>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
