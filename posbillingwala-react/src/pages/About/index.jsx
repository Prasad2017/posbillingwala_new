import { motion } from 'framer-motion'
import { Target, Eye, Sparkles, Users } from 'lucide-react'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import IconBadge from '../../components/common/IconBadge'
import Loader from '../../components/common/Loader'
import ErrorState from '../../components/common/ErrorState'
import { useApi } from '../../hooks/useApi'
import { getPageContent } from '../../services/websiteService'
import { useSettings } from '../../context/SettingsContext'
import { CMS_SLUGS } from '../../utils/constants'
import { formatDate } from '../../utils/helpers'
import logoFallback from '../../assets/images/logo/pos_billingwala_logo.png'
import aboutImage from '../../assets/images/business/services-image-03.jpg'
import aboutAccent from '../../assets/images/business/services-image-04.jpg'
import '../pages.css'
import './About.css'

const VALUES = [
  {
    icon: Target,
    tone: 'blue',
    title: 'Our Mission',
    text: 'Make professional billing accessible to every Indian business.',
  },
  {
    icon: Eye,
    tone: 'purple',
    title: 'Our Vision',
    text: 'Become the most trusted POS brand for local businesses.',
  },
  {
    icon: Sparkles,
    tone: 'amber',
    title: 'Our Promise',
    text: 'Simple software, reliable hardware and support that stays close.',
  },
  {
    icon: Users,
    tone: 'green',
    title: 'Our Network',
    text: 'Local dealers who help with setup, training and renewals.',
  },
]

export default function About() {
  const { logoUrl } = useSettings()
  const { data, loading, error, refetch } = useApi(
    () => getPageContent(CMS_SLUGS.about),
    [CMS_SLUGS.about],
  )

  return (
    <>
      <Seo
        title="About POS Billingwala"
        description="POS Billingwala is a complete POS solution for Indian businesses — software, hardware and local support."
        path="/about"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">ABOUT US</span>
          <h1>{data?.title || 'About POS Billingwala'}</h1>
          <p className="page-hero-lead">
            POS Billingwala is a complete POS solution for Indian businesses — software, hardware
            and local support.
          </p>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container about-story">
          <motion.div
            initial={{ opacity: 0, x: -24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <span className="section-title__eyebrow section-title__eyebrow--badge">Who we are</span>
            <h2 className="about-story__title page-heading">Smart billing for growing businesses</h2>
            <p className="about-story__lead">
              We help restaurants, retail stores, grocery shops and service businesses bill faster
              with reliable Android POS software and local dealer support.
            </p>
            <div className="about-values">
              {VALUES.map((v) => (
                <article key={v.title} className={`about-value about-value--${v.tone}`}>
                  <div className="about-value__icon">
                    <IconBadge icon={v.icon} tone={v.tone} />
                  </div>
                  <h3>{v.title}</h3>
                  <p>{v.text}</p>
                </article>
              ))}
            </div>
          </motion.div>

          <motion.div
            className="about-visual"
            initial={{ opacity: 0, x: 24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <div className="about-visual__glow" />
            <img className="about-visual__photo" src={aboutImage} alt="POS Billingwala team and product" loading="lazy" />
            <img className="about-visual__float" src={aboutAccent} alt="" loading="lazy" />
            <img className="about-visual__logo" src={logoUrl || logoFallback} alt="POS Billingwala" />
          </motion.div>
        </div>
      </section>

      <section className="section page-section--lavender">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Our story"
            title="Built for the Indian counter"
            subtitle="Practical tools, local partners and support that stays close to the business."
          />
          <article className="cms-card">
            {loading && <Loader label="Loading content…" />}
            {error && <ErrorState message={error} onRetry={refetch} />}
            {!loading && !error && data && (
              <div
                className="legal-body"
                dangerouslySetInnerHTML={{ __html: data.body_html || '' }}
              />
            )}
            {!loading && !error && !data && (
              <p className="text-muted">About content will appear here soon.</p>
            )}
            {data?.updated_at && (
              <p className="cms-updated">Last Updated: {formatDate(data.updated_at)}</p>
            )}
            <div className="cms-card__actions">
              <Button to="/contact" variant="primary">
                Contact Us
              </Button>
              <Button to="/download-app" variant="ghost">
                Download App
              </Button>
            </div>
          </article>

          <div className="cta-panel about-cta">
            <div>
              <h2>Ready to grow with us?</h2>
              <p>Talk to our team or download the app and start billing smarter.</p>
            </div>
            <div className="cta-panel__actions">
              <Button to="/contact" variant="red" size="large">
                Contact Us
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
