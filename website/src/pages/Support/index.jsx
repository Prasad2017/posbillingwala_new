import {
  MessageCircle,
  Phone,
  Mail,
  Settings,
  Printer,
  GraduationCap,
  MonitorSmartphone,
  RefreshCw,
  Headphones,
} from 'lucide-react'
import { motion } from 'framer-motion'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import AnimatedCard from '../../components/common/AnimatedCard'
import IconBadge from '../../components/common/IconBadge'
import FAQ from '../../components/support/FAQ'
import Loader from '../../components/common/Loader'
import ErrorState from '../../components/common/ErrorState'
import { useApi } from '../../hooks/useApi'
import { getPageContent } from '../../services/websiteService'
import { useSettings } from '../../context/SettingsContext'
import { CMS_SLUGS } from '../../utils/constants'
import { formatPhoneDisplay, telLink, whatsappLink, formatDate } from '../../utils/helpers'
import supportImage from '../../assets/images/business/services-image-03.jpg'
import supportAccent from '../../assets/images/business/services-image-04.jpg'
import '../pages.css'
import './Support.css'

const SERVICES = [
  { icon: Settings, title: 'Installation & Setup', text: 'App install and licence activation.', tone: 'blue' },
  { icon: Printer, title: 'Printer Setup', text: 'Bluetooth thermal printer pairing.', tone: 'orange' },
  { icon: GraduationCap, title: 'Billing Training', text: 'Staff training for daily billing.', tone: 'purple' },
  { icon: MonitorSmartphone, title: 'Remote Assistance', text: 'Phone and remote troubleshooting.', tone: 'cyan' },
  { icon: RefreshCw, title: 'Renewal Support', text: 'Licence renewal assistance.', tone: 'amber' },
  { icon: Headphones, title: 'WhatsApp Support', text: 'Quick help for common questions.', tone: 'green' },
]

export default function Support() {
  const { phone, whatsapp, email } = useSettings()
  const { data, loading, error, refetch } = useApi(
    () => getPageContent(CMS_SLUGS.support),
    [CMS_SLUGS.support],
  )

  const channels = [
    {
      key: 'wa',
      tone: 'green',
      href: whatsappLink(whatsapp, 'Hi, I need support with POS Billingwala.'),
      external: true,
      icon: MessageCircle,
      title: 'WhatsApp',
      text: 'Quick chat support',
    },
    {
      key: 'phone',
      tone: 'blue',
      href: telLink(phone),
      icon: Phone,
      title: 'Phone',
      text: formatPhoneDisplay(phone),
    },
    {
      key: 'email',
      tone: 'red',
      href: `mailto:${email}`,
      icon: Mail,
      title: 'Email',
      text: email,
    },
  ]

  return (
    <>
      <Seo
        title="We Are Here to Support You"
        description="App installation, printer setup and daily troubleshooting support for POS Billingwala."
        path="/support"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">CUSTOMER SUPPORT</span>
          <h1>We Are Here to Support You</h1>
          <p className="page-hero-lead">
            From app installation to printer setup and daily troubleshooting, our support team is
            here to help.
          </p>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container">
          <div className="support-intro">
            <motion.div
              initial={{ opacity: 0, x: -24 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <span className="section-title__eyebrow section-title__eyebrow--badge">How we help</span>
              <h2 className="support-heading page-heading">Complete support for your business</h2>
              <p className="support-lead">
                Whether you need setup help, printer pairing or daily billing guidance — we are just a
                message away.
              </p>
              <div className="page-cta-bar">
                <Button to="/contact?subject=Support%20Request" variant="primary">
                  Send a Message
                </Button>
              </div>
            </motion.div>

            <motion.div
              className="support-visual"
              initial={{ opacity: 0, x: 24 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <div className="support-visual__glow" />
              <img
                className="support-visual__main"
                src={supportImage}
                alt="POS Billingwala customer support"
                loading="lazy"
              />
              <img
                className="support-visual__float"
                src={supportAccent}
                alt=""
                loading="lazy"
              />
            </motion.div>
          </div>

          <div className="support-channel-grid">
            {channels.map((c, i) => (
              <motion.a
                key={c.key}
                className={`support-option support-option--${c.tone}`}
                href={c.href}
                target={c.external ? '_blank' : undefined}
                rel={c.external ? 'noopener noreferrer' : undefined}
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.06 }}
              >
                <div className="support-option__icon">
                  <IconBadge icon={c.icon} tone={c.tone === 'red' ? 'red' : c.tone} size="lg" />
                </div>
                <strong>{c.title}</strong>
                <span>{c.text}</span>
              </motion.a>
            ))}
          </div>
        </div>
      </section>

      <section className="section page-section--lavender">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Our services"
            title="What we support"
            subtitle="Practical help for setup, training and renewals."
          />
          <div className="support-service-grid">
            {SERVICES.map((s, i) => (
              <AnimatedCard
                key={s.title}
                className={`support-service support-service--${s.tone}`}
                delay={i * 0.04}
              >
                <div className="support-service__icon">
                  <IconBadge icon={s.icon} tone={s.tone} size="lg" />
                </div>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
              </AnimatedCard>
            ))}
          </div>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container support-faq">
          <SectionTitle
            colorful
            eyebrow="FAQ"
            title="Common questions"
            subtitle="Quick answers before you reach out."
          />
          <FAQ />
        </div>
      </section>

      {(loading || error || data) && (
        <section className="section">
          <div className="container">
            <SectionTitle
              colorful
              eyebrow="Support guide"
              title={data?.title || 'Support information'}
              subtitle="Everything you need for setup, troubleshooting and renewals."
            />
            <article className="cms-card support-guide-card">
              {loading && <Loader label="Loading support content…" />}
              {error && <ErrorState message={error} onRetry={refetch} />}
              {!loading && !error && data && (
                <div
                  className="legal-body"
                  dangerouslySetInnerHTML={{ __html: data.body_html || '' }}
                />
              )}
              {data?.updated_at && (
                <p className="cms-updated">Last Updated: {formatDate(data.updated_at)}</p>
              )}
              <div className="cms-card__actions">
                <Button to="/contact?subject=Support%20Request" variant="primary">
                  Contact Support
                </Button>
                <Button to="/dealers" variant="ghost">
                  Find a Dealer
                </Button>
              </div>
            </article>
          </div>
        </section>
      )}

      <section className="section">
        <div className="container">
          <div className="cta-panel support-cta">
            <div>
              <h2>Still need help?</h2>
              <p>Message us on WhatsApp or book a support call — we’re ready to assist.</p>
            </div>
            <div className="cta-panel__actions">
              <Button to="/contact?subject=Support%20Request" variant="red" size="large">
                Contact Support
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
