import { Swiper, SwiperSlide } from 'swiper/react'
import { Autoplay, Pagination } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/pagination'
import {
  Zap,
  Package,
  WifiOff,
  Printer,
  BarChart3,
  IndianRupee,
} from 'lucide-react'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import IconBadge from '../../components/common/IconBadge'
import AnimatedCard from '../../components/common/AnimatedCard'
import { useSettings } from '../../context/SettingsContext'
import appScreen from '../../assets/images/app/app-home-screen.png'
import posScreen from '../../assets/images/app/app-pos-screen.jpg'
import playBadge from '../../assets/images/app/google-play-badge.png'
import billingVisual from '../../assets/images/business/services-image-02.jpg'
import catalogVisual from '../../assets/images/business/services-image-04.jpg'
import '../pages.css'
import './Download.css'

const FEATURES = [
  { label: 'Fast Billing', icon: Zap, tone: 'amber' },
  { label: 'GST Ready', icon: IndianRupee, tone: 'green' },
  { label: 'Offline Mode', icon: WifiOff, tone: 'cyan' },
  { label: 'Printing', icon: Printer, tone: 'orange' },
  { label: 'Reports', icon: BarChart3, tone: 'purple' },
  { label: 'Inventory', icon: Package, tone: 'blue' },
]

const REQUIREMENTS = [
  'Android 6.0 or higher',
  'Minimum 2 GB RAM recommended',
  'Bluetooth for thermal printer support',
  'Internet for sync (offline billing supported)',
]

const SLIDES = [
  { src: appScreen, alt: 'POS Billingwala home screen', kind: 'phone' },
  { src: posScreen, alt: 'POS Billingwala billing modes', kind: 'phone' },
  { src: billingVisual, alt: 'Sales and billing dashboard', kind: 'panel' },
  { src: catalogVisual, alt: 'Catalog and inventory tools', kind: 'panel' },
]

export default function Download() {
  const { playStoreUrl, appVersion } = useSettings()

  return (
    <>
      <Seo
        title="Download POS Billingwala App"
        description="Download the Android POS app for fast billing, inventory, offline mode and Bluetooth printing."
        path="/download-app"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">DOWNLOAD APP</span>
          <h1>POS Billingwala In Your Pocket</h1>
          <p className="page-hero-lead">
            Run your business anywhere — fast billing, inventory, offline mode and Bluetooth printing.
          </p>
          <div className="page-cta-bar">
            <Button
              href={playStoreUrl}
              variant="red"
              size="large"
              target="_blank"
              rel="noopener noreferrer"
            >
              Download App
            </Button>
          </div>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container download-panel">
          <div className="download-card">
            <span className="section-title__eyebrow section-title__eyebrow--badge">Latest app</span>
            <h2 className="download-card__title page-heading">Billing made simple</h2>
            <p className="download-card__lead">
              Install POS Billingwala and start managing your counter from your Android phone or
              tablet.
            </p>
            <div className="download-feature-grid">
              {FEATURES.map((f) => (
                <AnimatedCard key={f.label} className={`download-feature download-feature--${f.tone}`}>
                  <div className="download-feature__icon">
                    <IconBadge icon={f.icon} tone={f.tone} />
                  </div>
                  <span>{f.label}</span>
                </AnimatedCard>
              ))}
            </div>
            <a
              className="download-badge"
              href={playStoreUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Android App on Google Play"
            >
              <img src={playBadge} alt="Android App on Google Play" />
            </a>
            {appVersion && <p className="download-version">Latest version: {appVersion}</p>}
            <div className="download-requirements">
              <h3>Device requirements</h3>
              <ul>
                {REQUIREMENTS.map((r) => (
                  <li key={r}>{r}</li>
                ))}
              </ul>
            </div>
          </div>

          <div className="download-visual">
            <div className="download-visual__glow" />
            <Swiper
              modules={[Autoplay, Pagination]}
              autoplay={{ delay: 3500, disableOnInteraction: false }}
              pagination={{ clickable: true }}
              spaceBetween={16}
              slidesPerView={1}
              className="download-swiper"
            >
              {SLIDES.map((slide) => (
                <SwiperSlide key={slide.alt}>
                  <div className={`download-slide download-slide--${slide.kind}`}>
                    <img src={slide.src} alt={slide.alt} loading="lazy" />
                  </div>
                </SwiperSlide>
              ))}
            </Swiper>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Ready when you are"
            title="Start billing in minutes"
            subtitle="Download from Google Play and set up with your dealer or our support team."
          />
          <div className="cta-panel">
            <div>
              <h2>Get POS Billingwala today</h2>
              <p>One app for billing, inventory, GST and reports.</p>
            </div>
            <div className="cta-panel__actions">
              <Button
                href={playStoreUrl}
                variant="yellow"
                size="large"
                target="_blank"
                rel="noopener noreferrer"
              >
                Download App
              </Button>
              <Button to="/contact?subject=App%20Setup%20Help" variant="outline" size="large">
                Need Setup Help?
              </Button>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
