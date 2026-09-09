import { motion } from 'framer-motion'
import CountUpImport from 'react-countup'
import { Link } from 'react-router-dom'
import {
  Zap,
  Package,
  IndianRupee,
  Printer,
  BarChart3,
  Users,
  Download,
  Settings,
  Receipt,
  ArrowRight,
  Star,
  UtensilsCrossed,
  ShoppingCart,
  Store,
  Pill,
  Coffee,
  Cake,
  Shirt,
  Building2,
  Boxes,
  Soup,
  Sparkles,
  WifiOff,
  Cloud,
} from 'lucide-react'
import { Swiper, SwiperSlide } from 'swiper/react'
import { Autoplay } from 'swiper/modules'
import 'swiper/css'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import SectionTitle from '../../components/common/SectionTitle'
import AnimatedCard from '../../components/common/AnimatedCard'
import Badge from '../../components/common/Badge'
import IconBadge from '../../components/common/IconBadge'
import ProductCard from '../../components/products/ProductCard'
import { CardSkeleton } from '../../components/common/Skeleton'
import ErrorState from '../../components/common/ErrorState'
import { useApi } from '../../hooks/useApi'
import { getProducts } from '../../services/productService'
import { getPricing } from '../../services/pricingService'
import { getDealers } from '../../services/dealerService'
import { getCustomers, getTestimonials } from '../../services/websiteService'
import { BUSINESS_TYPES } from '../../utils/constants'
import { useSettings } from '../../context/SettingsContext'
import appScreen from '../../assets/images/app/app-home-screen.png'
import playBadge from '../../assets/images/app/google-play-badge.png'
import './Home.css'

const CountUp = CountUpImport.default || CountUpImport

const fadeUp = {
  hidden: { opacity: 0, y: 28 },
  show: (i = 0) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, delay: i * 0.08, ease: [0.22, 1, 0.36, 1] },
  }),
}

const INDUSTRY_ICONS = {
  UtensilsCrossed,
  ShoppingCart,
  Store,
  Pill,
  Coffee,
  Cake,
  Shirt,
  Building2,
  Boxes,
  Soup,
}

const FEATURES = [
  { icon: Zap, title: 'Fast Billing', text: 'Generate bills quickly at the counter.', tone: 'amber', accent: 'amber' },
  { icon: Package, title: 'Smart Inventory', text: 'Track stock easily every day.', tone: 'purple', accent: 'purple' },
  { icon: IndianRupee, title: 'GST Ready', text: 'Manage GST billing with confidence.', tone: 'green', accent: 'green' },
  { icon: Printer, title: 'Print Anywhere', text: 'Bluetooth & USB printer support.', tone: 'cyan', accent: 'cyan' },
  { icon: BarChart3, title: 'Business Reports', text: 'Understand your growth clearly.', tone: 'blue', accent: 'blue' },
  { icon: Users, title: 'Customers', text: 'Manage customer information easily.', tone: 'pink', accent: 'pink' },
]

const STEPS = [
  { n: '01', title: 'Download', text: 'Install the Android app from Play Store.', icon: Download, tone: 'blue' },
  { n: '02', title: 'Setup', text: 'Add products, printers and business details.', icon: Settings, tone: 'orange' },
  { n: '03', title: 'Start Billing', text: 'Bill faster and grow with confidence.', icon: Receipt, tone: 'green' },
]

const INDUSTRY_TONES = ['red', 'green', 'blue', 'cyan', 'amber', 'orange', 'pink', 'indigo', 'purple', 'teal']
const SOFTWARE_FEATURES = [
  { label: 'Fast & Easy Billing', icon: Zap, tone: 'amber' },
  { label: 'Inventory Management', icon: Package, tone: 'purple' },
  { label: 'GST Billing & Invoices', icon: IndianRupee, tone: 'green' },
  { label: 'Sales Reports & Analytics', icon: BarChart3, tone: 'blue' },
  { label: 'Customer Management', icon: Users, tone: 'pink' },
  { label: 'Offline Mode & Auto Sync', icon: WifiOff, tone: 'cyan' },
  { label: 'Bluetooth Thermal Printing', icon: Printer, tone: 'orange' },
]

export default function Home() {
  const { playStoreUrl } = useSettings()
  const productsApi = useApi(getProducts, [])
  const pricingApi = useApi(getPricing, [])
  const dealersApi = useApi(getDealers, [])
  const clientsApi = useApi(getCustomers, [])
  const testimonialsApi = useApi(getTestimonials, [])

  const featuredProducts = (productsApi.data || []).slice(0, 4)
  const stats = {
    businesses: clientsApi.data?.length || 0,
    products: productsApi.data?.length || 0,
    plans: pricingApi.data?.length || 0,
    dealers: dealersApi.data?.length || 0,
  }

  return (
    <>
      <Seo
        title="Smart Billing. Trusted Support. Better Business."
        description="Modern POS & billing solution for restaurants, retail, grocery and growing Indian businesses."
        path="/"
      />

      <section className="home-hero">
        <div className="home-hero__bg" />
        <div className="home-hero__glow home-hero__glow--1" />
        <div className="home-hero__glow home-hero__glow--2" />
        <div className="home-hero__glow home-hero__glow--3" />
        <div className="home-hero__orb home-hero__orb--1" />
        <div className="home-hero__orb home-hero__orb--2" />
        <div className="home-hero__orb home-hero__orb--3" />

        <div className="container home-hero__grid">
          <div className="home-hero__copy">
            <motion.div custom={0} variants={fadeUp} initial="hidden" animate="show">
              <Badge tone="light">
                <Sparkles size={14} /> Smart POS for Indian Businesses
              </Badge>
            </motion.div>
            <motion.h1 custom={1} variants={fadeUp} initial="hidden" animate="show">
              Smart Billing
              <br />
              <span className="home-hero__accent">For Smart Businesses</span>
            </motion.h1>
            <motion.p custom={2} variants={fadeUp} initial="hidden" animate="show">
              Modern POS & Billing Solution For Every Business — fast, simple and dealer-supported.
            </motion.p>
            <motion.div
              className="home-hero__cta"
              custom={3}
              variants={fadeUp}
              initial="hidden"
              animate="show"
            >
              <Button to="/contact" variant="red" size="large">
                Book Free Demo
              </Button>
              <a
                className="home-hero__store"
                href={playStoreUrl}
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Android App on Google Play"
              >
                <img src={playBadge} alt="Android App on Google Play" />
              </a>
            </motion.div>
            <motion.div
              className="home-hero__chips"
              custom={4}
              variants={fadeUp}
              initial="hidden"
              animate="show"
            >
              <span className="home-chip home-chip--green">GST Ready</span>
              <span className="home-chip home-chip--blue">Offline Mode</span>
              <span className="home-chip home-chip--orange">Printer Support</span>
            </motion.div>
          </div>

          <motion.div
            className="home-hero__visual"
            initial={{ opacity: 0, scale: 0.92 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.7, delay: 0.15, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="home-hero__ring" />
            <motion.div className="home-phone float-y" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.6, delay: 0.2 }}>
              <img src={appScreen} alt="POS Billingwala app screenshot" />
            </motion.div>

            <motion.div
              className="home-float home-float--sales float-y-delay"
              initial={{ opacity: 0, x: -24 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.35, duration: 0.45 }}
            >
              <IconBadge icon={IndianRupee} tone="green" size="sm" />
              <div>
                <strong>Sales</strong>
                <span>Live billing</span>
              </div>
            </motion.div>

            <motion.div
              className="home-float home-float--reports float-y"
              initial={{ opacity: 0, x: 24 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.45, duration: 0.45 }}
            >
              <IconBadge icon={BarChart3} tone="blue" size="sm" />
              <div>
                <strong>Reports</strong>
                <span>Daily insights</span>
              </div>
            </motion.div>

            <motion.div
              className="home-float home-float--gst float-y-delay"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.55, duration: 0.45 }}
            >
              <IconBadge icon={Receipt} tone="amber" size="sm" />
              <div>
                <strong>GST</strong>
                <span>Tax ready</span>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </section>

      <section className="section home-trust">
        <motion.div
          className="container home-trust__grid"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
        >
          {[
            { label: 'Businesses', value: stats.businesses, suffix: '+', icon: Users, tone: 'blue' },
            { label: 'Products', value: stats.products, suffix: '+', icon: Package, tone: 'purple' },
            { label: 'Plans', value: stats.plans, suffix: '', icon: IndianRupee, tone: 'green' },
            { label: 'Dealer Locations', value: stats.dealers, suffix: '+', icon: Store, tone: 'orange' },
          ].map((item, i) => (
            <motion.div
              key={item.label}
              className={`home-trust__item home-trust__item--${item.tone}`}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.08, duration: 0.4 }}
              whileHover={{ y: -8, scale: 1.03 }}
            >
              <div className="home-trust__top">
                <div className="home-trust__icon">
                  <item.icon size={22} strokeWidth={2.2} />
                </div>
                <strong>
                  {item.value > 0 ? (
                    <CountUp end={item.value} duration={1.6} suffix={item.suffix} />
                  ) : (
                    '—'
                  )}
                </strong>
              </div>
              <span>{item.label}</span>
            </motion.div>
          ))}
        </motion.div>
      </section>

      <section className="section home-section--features">
        <div className="container">
          <SectionTitle
            colorful
            singleLine
            eyebrow="Why Choose POS Billingwala"
            title="Built for Speed, Trust & Growth"
            subtitle="Everything you need to run daily billing — fast, simple and powerful."
          />
          <div className="home-features">
            {FEATURES.map((f, i) => (
              <AnimatedCard
                key={f.title}
                className={`home-feature home-feature--${f.accent}`}
                delay={i * 0.06}
              >
                <IconBadge icon={f.icon} tone={f.tone} />
                <h3>{f.title}</h3>
                <p>{f.text}</p>
              </AnimatedCard>
            ))}
          </div>
        </div>
      </section>

      <section className="section home-section--industries">
        <div className="container">
          <SectionTitle
            colorful
            singleLine
            eyebrow="Made for Every Business"
            title="One POS. Many Industries."
            subtitle="Designed for the way Indian businesses actually work."
          />
          <div className="home-industries">
            {BUSINESS_TYPES.map((b, i) => {
              const Icon = INDUSTRY_ICONS[b.icon] || Package
              return (
                <AnimatedCard
                  key={b.id}
                  className={`home-industry home-industry--${INDUSTRY_TONES[i % INDUSTRY_TONES.length]}`}
                  delay={i * 0.04}
                >
                  <IconBadge icon={Icon} tone={INDUSTRY_TONES[i % INDUSTRY_TONES.length]} />
                  <span>{b.label}</span>
                </AnimatedCard>
              )
            })}
          </div>
        </div>
      </section>

      <section className="section section--muted">
        <div className="container">
          <SectionTitle
            colorful
            singleLine
            eyebrow="Featured Products"
            title="Everything your counter needs"
            subtitle="Software, printers, rolls and accessories for every counter."
          />
          {productsApi.loading && (
            <div className="home-products">
              {Array.from({ length: 4 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          )}
          {productsApi.error && (
            <ErrorState message={productsApi.error} onRetry={productsApi.refetch} />
          )}
          {!productsApi.loading && !productsApi.error && (
            <>
              <div className="home-products">
                {featuredProducts.map((p, i) => (
                  <ProductCard key={p.id} product={p} delay={i * 0.05} />
                ))}
              </div>
              <div className="home-center-cta">
                <Link to="/products" className="text-link">
                  View all products <ArrowRight size={16} />
                </Link>
              </div>
            </>
          )}
        </div>
      </section>

      <section className="section home-section--steps">
        <div className="container">
          <SectionTitle
            colorful
            singleLine
            eyebrow="How It Works"
            title="Start Billing in 3 Steps"
            subtitle="Download, set up, and start billing in minutes."
          />
          <div className="home-steps">
            {STEPS.map((s, i) => (
              <AnimatedCard key={s.n} className={`home-step home-step--${s.tone}`} delay={i * 0.1}>
                <div className="home-step__icon">
                  <IconBadge icon={s.icon} tone={s.tone} size="lg" />
                  <span className="home-step__n">{s.n}</span>
                </div>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
              </AnimatedCard>
            ))}
          </div>
        </div>
      </section>

      <section className="section home-section--software">
        <div className="container home-split">
          <motion.div
            initial={{ opacity: 0, x: -28 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
          >
            <span className="section-title__eyebrow section-title__eyebrow--badge">Software Features</span>
            <h2 className="home-split__title">Complete Business Control in One App</h2>
            <p className="home-split__lead">
              Bill faster, manage stock, print receipts and track sales — all from your Android phone or tablet.
            </p>
            <ul className="home-split__list">
              {SOFTWARE_FEATURES.map((f, i) => (
                <motion.li
                  key={f.label}
                  className={`home-split__item home-split__item--${f.tone}`}
                  initial={{ opacity: 0, x: -16 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.05 }}
                >
                  <IconBadge icon={f.icon} tone={f.tone} size="sm" />
                  <span>{f.label}</span>
                </motion.li>
              ))}
            </ul>
            <div className="home-split__actions">
              <Button to="/software" variant="primary">
                Explore Software
              </Button>
              <Button to="/download-app" variant="ghost">
                Download App
              </Button>
            </div>
          </motion.div>

          <motion.div
            className="home-split__showcase"
            initial={{ opacity: 0, x: 28 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.55 }}
          >
            <div className="home-split__glow" />
            <div className="home-split__phone float-y">
              <img src={appScreen} alt="POS Billingwala software dashboard" loading="lazy" />
            </div>
            <div className="home-split__chip home-split__chip--gst float-y-delay">
              <IconBadge icon={IndianRupee} tone="green" size="sm" />
              <span>GST Ready</span>
            </div>
            <div className="home-split__chip home-split__chip--print">
              <IconBadge icon={Printer} tone="orange" size="sm" />
              <span>Print Bills</span>
            </div>
            <div className="home-split__chip home-split__chip--offline float-y">
              <IconBadge icon={Cloud} tone="cyan" size="sm" />
              <span>Cloud Sync</span>
            </div>
          </motion.div>
        </div>
      </section>

      {(testimonialsApi.loading ||
        (testimonialsApi.data && testimonialsApi.data.length > 0)) && (
        <section className="section home-section--quotes">
          <div className="container">
            <SectionTitle
              colorful
              singleLine
              eyebrow="Testimonials"
              title="Trusted by Growing Businesses"
              subtitle="Real feedback from published customer reviews."
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
                    <article className="home-quote">
                      <div className="home-quote__stars">
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
        <motion.div
          className="container home-download"
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
        >
          <div className="home-download__top">
            <h2 className="home-download__title">
              Download POS BillingWala and manage your entire business from one powerful app.
            </h2>
            <a
              className="home-download__store"
              href={playStoreUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Android App on Google Play"
            >
              <img src={playBadge} alt="Android App on Google Play" />
            </a>
          </div>
          <div className="home-download__copy">
            <p>
              Create GST bills in seconds, manage thousands of products, track stock in real time, and get
              alerts before important items run out.
            </p>
            <p>
              Monitor daily sales, expenses, profit, customer payments, and business performance with clear
              reports that help you make smarter decisions.
            </p>
            <p>
              Save time, reduce billing mistakes, control inventory, improve customer service, and grow your
              shop, restaurant, supermarket, or business with confidence.
            </p>
          </div>
        </motion.div>
      </section>
    </>
  )
}
