import { motion } from 'framer-motion'
import {
  Zap,
  Package,
  IndianRupee,
  Printer,
  BarChart3,
  WifiOff,
  Shield,
  Users,
  Cloud,
  Check,
} from 'lucide-react'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import AnimatedCard from '../../components/common/AnimatedCard'
import IconBadge from '../../components/common/IconBadge'
import { useSettings } from '../../context/SettingsContext'
import appScreen from '../../assets/images/app/app-home-screen.png'
import posScreen from '../../assets/images/app/app-pos-screen.jpg'
import reportsVisual from '../../assets/images/business/services-image-02.jpg'
import inventoryVisual from '../../assets/images/business/services-image-04.jpg'
import catalogVisual from '../../assets/images/business/services-image-03.jpg'
import stockVisual from '../../assets/images/business/services-image.jpg'
import '../pages.css'
import './Software.css'

const FEATURES = [
  { label: 'Fast & Easy Billing', icon: Zap, tone: 'amber' },
  { label: 'Inventory Management', icon: Package, tone: 'purple' },
  { label: 'GST Billing & Invoices', icon: IndianRupee, tone: 'green' },
  { label: 'Sales Reports & Analytics', icon: BarChart3, tone: 'blue' },
  { label: 'Customer Management', icon: Users, tone: 'pink' },
  { label: 'Offline Mode & Auto Sync', icon: WifiOff, tone: 'cyan' },
  { label: 'Bluetooth Thermal Printing', icon: Printer, tone: 'orange' },
]

const SHOWCASE = [
  {
    step: '01',
    eyebrow: 'Billing',
    title: 'Create GST bills in seconds',
    text: 'Serve customers faster with Fast Billing, Dine In, Take Away and Mess modes — plus GST invoices ready to print.',
    points: [
      'Quick item search at the counter',
      'Cash, UPI & mixed payments',
      'Bluetooth thermal receipts',
    ],
    icon: Zap,
    tone: 'amber',
    image: appScreen,
    imageAlt: 'POS Billingwala home screen with billing modes',
    frame: 'phone',
  },
  {
    step: '02',
    eyebrow: 'Inventory',
    title: 'Control stock before it runs out',
    text: 'Manage thousands of products from your phone. Update rates, track categories and stay ready for every rush hour.',
    points: [
      'Products, categories & combos',
      'Live stock visibility',
      'Easy updates that sync',
    ],
    icon: Package,
    tone: 'purple',
    reverse: true,
    image: inventoryVisual,
    imageAlt: 'Inventory and product catalog management illustration',
    frame: 'panel',
    secondaryImage: catalogVisual,
  },
  {
    step: '03',
    eyebrow: 'Reports',
    title: 'See sales, profit and growth clearly',
    text: 'Monitor daily sales, expenses, customer payments and business performance with reports that help you decide smarter.',
    points: [
      'Daily & period sales summaries',
      'Expense and profit clarity',
      'Customer payment tracking',
    ],
    icon: BarChart3,
    tone: 'blue',
    image: reportsVisual,
    imageAlt: 'Business sales and analytics dashboard illustration',
    frame: 'panel',
    secondaryImage: stockVisual,
  },
]

const HIGHLIGHTS = [
  { icon: Zap, title: 'Easy to Use', text: 'Clean interface — staff can start billing quickly.', tone: 'amber' },
  { icon: WifiOff, title: 'Works Offline', text: 'Keep billing when internet is unavailable.', tone: 'cyan' },
  { icon: Shield, title: 'Secure Data', text: 'Your business data stays safe and synced.', tone: 'green' },
  { icon: Printer, title: 'Bluetooth Printing', text: 'Print receipts on supported thermal printers.', tone: 'orange' },
  { icon: IndianRupee, title: 'GST Ready', text: 'Professional invoices with tax details.', tone: 'blue' },
  { icon: Users, title: 'Local Support', text: 'Dealer network for setup and help.', tone: 'pink' },
]

export default function Software() {
  const { playStoreUrl } = useSettings()

  return (
    <>
      <Seo
        title="Powerful Billing Software"
        description="Fast, easy and reliable POS billing for restaurants, retail, grocery, mess and other Indian businesses."
        path="/software"
      />

      <section className="page-hero software-hero">
        <div className="container">
          <span className="software-hero__badge">POS SOFTWARE</span>
          <h1>Powerful Billing Software</h1>
          <p className="page-hero-lead">
            Fast, easy and reliable POS billing for restaurants, retail, grocery, mess and other
            Indian businesses.
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
            <Button to="/contact?subject=Software%20Demo" variant="outline" size="large">
              Book Free Demo
            </Button>
          </div>
        </div>
      </section>

      <section className="section software-section--intro">
        <div className="container software-intro">
          <motion.div
            initial={{ opacity: 0, x: -24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <span className="section-title__eyebrow section-title__eyebrow--badge">Built for your team</span>
            <h2 className="software-intro__title">One app for daily business</h2>
            <p className="software-intro__lead">
              Bill faster, manage products, print receipts and understand your sales — all from your
              Android phone or tablet.
            </p>
            <ul className="software-feature-grid">
              {FEATURES.map((f, i) => (
                <motion.li
                  key={f.label}
                  className={`software-feature software-feature--${f.tone}`}
                  initial={{ opacity: 0, x: -16 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.04 }}
                >
                  <IconBadge icon={f.icon} tone={f.tone} size="sm" />
                  <span>{f.label}</span>
                </motion.li>
              ))}
            </ul>
            <div className="page-cta-bar">
              <Button to="/download-app" variant="primary">
                Get the App
              </Button>
              <Button to="/pricing" variant="ghost">
                View Pricing
              </Button>
            </div>
          </motion.div>

          <motion.div
            className="software-phone-stage"
            initial={{ opacity: 0, x: 24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <div className="software-phone-stage__glow" />
            <div className="software-phone float-y">
              <img src={appScreen} alt="POS Billingwala app dashboard" loading="lazy" />
            </div>
            <div className="software-phone software-phone--secondary float-y-delay">
              <img src={posScreen} alt="POS Billingwala billing modes" loading="lazy" />
            </div>
            <div className="software-chip software-chip--gst float-y-delay">
              <IconBadge icon={IndianRupee} tone="green" size="sm" />
              <span>GST Ready</span>
            </div>
            <div className="software-chip software-chip--print">
              <IconBadge icon={Printer} tone="orange" size="sm" />
              <span>Print Bills</span>
            </div>
            <div className="software-chip software-chip--sync float-y">
              <IconBadge icon={Cloud} tone="cyan" size="sm" />
              <span>Cloud Sync</span>
            </div>
          </motion.div>
        </div>
      </section>

      <section className="section software-section--showcase">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Core capabilities"
            title="Everything your counter needs"
            subtitle="Billing, stock and reports — designed for restaurants, retail shops and growing Indian businesses."
          />
          <div className="software-dive">
            {SHOWCASE.map((block, i) => (
              <motion.article
                key={block.title}
                className={`software-dive__card software-dive__card--${block.tone}${block.reverse ? ' software-dive__card--reverse' : ''}`}
                initial={{ opacity: 0, y: 32 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.2 }}
                transition={{ duration: 0.5, delay: i * 0.06 }}
              >
                <div className="software-dive__copy">
                  <div className="software-dive__meta">
                    <span className={`software-dive__step software-dive__step--${block.tone}`}>
                      {block.step}
                    </span>
                    <span className={`software-dive__eyebrow software-dive__eyebrow--${block.tone}`}>
                      <IconBadge icon={block.icon} tone={block.tone} size="sm" />
                      {block.eyebrow}
                    </span>
                  </div>
                  <h3>{block.title}</h3>
                  <p>{block.text}</p>
                  <ul className="software-dive__points">
                    {block.points.map((point) => (
                      <li key={point}>
                        <span className={`software-dive__check software-dive__check--${block.tone}`}>
                          <Check size={14} strokeWidth={3} />
                        </span>
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                <div className={`software-dive__media software-dive__media--${block.tone}`}>
                  {block.frame === 'phone' ? (
                    <div className="software-dive__phone">
                      <img src={block.image} alt={block.imageAlt} loading="lazy" />
                    </div>
                  ) : (
                    <div className="software-dive__panel">
                      <img src={block.image} alt={block.imageAlt} loading="lazy" />
                      {block.secondaryImage && (
                        <img
                          className="software-dive__panel-float"
                          src={block.secondaryImage}
                          alt=""
                          loading="lazy"
                        />
                      )}
                    </div>
                  )}
                </div>
              </motion.article>
            ))}
          </div>
        </div>
      </section>

      <section className="section software-section--highlights">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Why choose us"
            title="Why businesses love our software"
            subtitle="Simple, reliable and built for Indian counters."
          />
          <div className="software-highlights">
            {HIGHLIGHTS.map((f, i) => (
              <AnimatedCard
                key={f.title}
                className={`software-tile software-tile--${f.tone}`}
                delay={i * 0.04}
              >
                <IconBadge icon={f.icon} tone={f.tone} />
                <h3>{f.title}</h3>
                <p>{f.text}</p>
              </AnimatedCard>
            ))}
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="software-cta">
            <div className="software-cta__glow" />
            <div className="software-cta__copy">
              <span className="software-cta__badge">Get started</span>
              <h2 className="software-cta__title">Ready to simplify billing?</h2>
              <p>Download the app and start billing in minutes — GST ready, offline friendly, and built for busy counters.</p>
            </div>
            <div className="software-cta__actions">
              <Button
                href={playStoreUrl}
                variant="yellow"
                size="large"
                target="_blank"
                rel="noopener noreferrer"
              >
                Download App
              </Button>
              <Button to="/contact?subject=Book%20Demo" variant="outline" size="large">
                Book Demo
              </Button>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
