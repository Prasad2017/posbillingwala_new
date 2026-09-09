import { formatPrice } from '../../utils/helpers'
import './PricingComparison.css'

export default function PricingComparison({ plans }) {
  if (!plans?.length) return null

  return (
    <div className="pricing-compare">
      <div className="pricing-compare__table hide-mobile">
        <table>
          <thead>
            <tr>
              <th>Plan</th>
              <th>Type</th>
              <th>Price</th>
              <th>Note</th>
            </tr>
          </thead>
          <tbody>
            {plans.map((plan) => (
              <tr key={plan.id} className={plan.is_featured ? 'is-featured' : ''}>
                <td>{plan.validity_label}</td>
                <td>{plan.plan_type === 'renewal' ? 'Renewal' : 'Subscription'}</td>
                <td>{formatPrice(plan.price)}</td>
                <td>{plan.gst_note || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="pricing-compare__cards hide-desktop">
        {plans.map((plan) => (
          <article key={plan.id} className={plan.is_featured ? 'is-featured' : ''}>
            <h4>{plan.validity_label}</h4>
            <p>
              <strong>{formatPrice(plan.price)}</strong>
            </p>
            <p>{plan.plan_type === 'renewal' ? 'Renewal' : 'Subscription'}</p>
            {plan.gst_note && <p className="text-muted">{plan.gst_note}</p>}
          </article>
        ))}
      </div>
    </div>
  )
}
