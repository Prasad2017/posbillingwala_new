import { Check } from 'lucide-react'
import Button from '../common/Button'
import AnimatedCard from '../common/AnimatedCard'
import { formatPrice, splitFeatures } from '../../utils/helpers'
import './PricingCard.css'

export default function PricingCard({ plan, delay = 0 }) {
  const features = splitFeatures(plan.description)
  const featured = Boolean(plan.is_featured)

  return (
    <AnimatedCard
      className={`pricing-card${featured ? ' pricing-card--featured' : ''}`}
      delay={delay}
    >
      {featured && <span className="pricing-card__badge">Most Popular</span>}
      <div className="pricing-card__type">
        {plan.plan_type === 'renewal' ? 'Renewal' : 'Subscription'}
      </div>
      <h3>{plan.validity_label || 'Plan'}</h3>
      <div className="pricing-card__price">{formatPrice(plan.price)}</div>
      {plan.gst_note && <p className="pricing-card__gst">{plan.gst_note}</p>}
      <ul className="pricing-card__features">
        {features.length > 0 ? (
          features.map((f) => (
            <li key={f}>
              <Check size={16} /> {f}
            </li>
          ))
        ) : (
          <li>
            <Check size={16} /> Contact for full feature list
          </li>
        )}
      </ul>
      <Button to="/contact?subject=Get%20Started%20Pricing" variant={featured ? 'red' : 'primary'} fullWidth>
        Get Started
      </Button>
    </AnimatedCard>
  )
}
