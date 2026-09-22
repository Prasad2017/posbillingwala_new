import { useMemo, useState } from 'react'
import Seo from '../../components/common/Seo'
import SectionTitle from '../../components/common/SectionTitle'
import PricingCard from '../../components/pricing/PricingCard'
import { CardSkeleton } from '../../components/common/Skeleton'
import ErrorState from '../../components/common/ErrorState'
import EmptyState from '../../components/common/EmptyState'
import Button from '../../components/common/Button'
import { useApi } from '../../hooks/useApi'
import { getPricing } from '../../services/pricingService'
import '../pages.css'
import './Pricing.css'

export default function Pricing() {
  const { data, loading, error, refetch } = useApi(getPricing, [])
  const plans = data || []
  const [filter, setFilter] = useState('all')

  const hasPlanTypes = useMemo(
    () => plans.some((p) => p.plan_type === 'subscription' || p.plan_type === 'renewal'),
    [plans],
  )

  const filtered = useMemo(() => {
    if (!hasPlanTypes || filter === 'all') return plans
    return plans.filter((p) => {
      const type = p.plan_type === 'renewal' ? 'renewal' : 'subscription'
      return type === filter
    })
  }, [plans, filter, hasPlanTypes])

  return (
    <>
      <Seo
        title="Simple Pricing. Flexible Plans."
        description="Choose a POS Billingwala plan that fits your business. Contact your dealer for the current quote."
        path="/pricing"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">PRICING</span>
          <h1>Simple Pricing. Flexible Plans.</h1>
          <p className="page-hero-lead">
            Choose a plan that fits your business. Contact your dealer for the exact current quote.
          </p>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container">
          <SectionTitle
            colorful
            eyebrow="Software plans"
            title="Transparent pricing for every business"
            subtitle="Plans include GST billing tools, offline mode and support pathways."
          />

          {hasPlanTypes && !loading && !error && plans.length > 0 && (
            <div className="filter-tabs pricing-tabs" role="tablist" aria-label="Plan type">
              {[
                { id: 'all', label: 'All plans' },
                { id: 'subscription', label: 'Subscription' },
                { id: 'renewal', label: 'Renewal' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  className={filter === tab.id ? 'is-active' : ''}
                  onClick={() => setFilter(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          )}

          {loading && (
            <div className="pricing-page__row">
              {Array.from({ length: 3 }).map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          )}

          {error && <ErrorState message={error} onRetry={refetch} />}

          {!loading && !error && plans.length === 0 && (
            <EmptyState
              title="Pricing coming soon"
              message="Plans will appear here soon."
            />
          )}

          {!loading && !error && filtered.length > 0 && (
            <div className="pricing-page__row">
              {filtered.map((plan, i) => (
                <PricingCard key={plan.id} plan={plan} delay={Math.min(i * 0.05, 0.3)} />
              ))}
            </div>
          )}

          {!loading && !error && plans.length > 0 && filtered.length === 0 && (
            <EmptyState title="No plans in this category" message="Try another filter." />
          )}
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="cta-panel">
            <div>
              <h2>Need a custom quote?</h2>
              <p>Talk to us or your nearest dealer for the right plan and setup.</p>
            </div>
            <div className="cta-panel__actions">
              <Button to="/contact?subject=Get%20Started%20Pricing" variant="red" size="large">
                Get Started
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
