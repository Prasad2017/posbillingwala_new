import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AnimatePresence } from 'framer-motion'
import { HelmetProvider } from 'react-helmet-async'
import { SettingsProvider } from './context/SettingsContext'
import Header from './components/layout/Header'
import Footer from './components/layout/Footer'
import ScrollToTop from './components/layout/ScrollToTop'
import HeaderTheme from './components/layout/HeaderTheme'
import PageTransition from './components/layout/PageTransition'
import FloatingWhatsApp from './components/common/FloatingWhatsApp'
import PageLoader from './components/common/PageLoader'
import { LEGACY_REDIRECTS } from './utils/constants'

const Home = lazy(() => import('./pages/Home'))
const Products = lazy(() => import('./pages/Products'))
const Software = lazy(() => import('./pages/Software'))
const Pricing = lazy(() => import('./pages/Pricing'))
const Dealers = lazy(() => import('./pages/Dealers'))
const Customers = lazy(() => import('./pages/Customers'))
const Support = lazy(() => import('./pages/Support'))
const Download = lazy(() => import('./pages/Download'))
const Contact = lazy(() => import('./pages/Contact'))
const Company = lazy(() => import('./pages/Company'))
const About = lazy(() => import('./pages/About'))
const Privacy = lazy(() => import('./pages/Privacy'))
const Terms = lazy(() => import('./pages/Terms'))
const Refund = lazy(() => import('./pages/Refund'))

function AnimatedRoutes() {
  const location = useLocation()

  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route
          path="/"
          element={
            <PageTransition>
              <Home />
            </PageTransition>
          }
        />
        <Route path="/products" element={<PageTransition><Products /></PageTransition>} />
        <Route path="/software" element={<PageTransition><Software /></PageTransition>} />
        <Route path="/pricing" element={<PageTransition><Pricing /></PageTransition>} />
        <Route path="/dealers" element={<PageTransition><Dealers /></PageTransition>} />
        <Route path="/customers" element={<PageTransition><Customers /></PageTransition>} />
        <Route path="/support" element={<PageTransition><Support /></PageTransition>} />
        <Route path="/download-app" element={<PageTransition><Download /></PageTransition>} />
        <Route path="/contact" element={<PageTransition><Contact /></PageTransition>} />
        <Route path="/company" element={<PageTransition><Company /></PageTransition>} />
        <Route path="/about" element={<PageTransition><About /></PageTransition>} />
        <Route path="/privacy-policy" element={<PageTransition><Privacy /></PageTransition>} />
        <Route path="/terms-conditions" element={<PageTransition><Terms /></PageTransition>} />
        <Route path="/refund-policy" element={<PageTransition><Refund /></PageTransition>} />
        {Object.entries(LEGACY_REDIRECTS).map(([from, to]) => (
          <Route key={from} path={from} element={<Navigate to={to} replace />} />
        ))}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AnimatePresence>
  )
}

export default function App() {
  return (
    <HelmetProvider>
      <SettingsProvider>
        <BrowserRouter>
          <ScrollToTop />
          <HeaderTheme />
          <div className="app-shell">
            <Header />
            <main className="app-main">
              <Suspense fallback={<PageLoader />}>
                <AnimatedRoutes />
              </Suspense>
            </main>
            <Footer />
            <FloatingWhatsApp />
          </div>
        </BrowserRouter>
      </SettingsProvider>
    </HelmetProvider>
  )
}
