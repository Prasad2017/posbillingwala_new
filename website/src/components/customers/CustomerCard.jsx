import AnimatedCard from '../common/AnimatedCard'
import { safeExternalUrl } from '../../utils/helpers'
import './CustomerCard.css'

export default function CustomerCard({ client, delay = 0 }) {
  const cta = safeExternalUrl(client.cta_url)
  const meta = [client.city, client.business_category].filter(Boolean).join(' · ')

  return (
    <AnimatedCard className="customer-card" delay={delay}>
      <div
        className="customer-card__photo"
        style={client.photo_url ? { backgroundImage: `url(${client.photo_url})` } : undefined}
      />
      <div className="customer-card__body">
        {client.logo_url && (
          <img className="customer-card__logo" src={client.logo_url} alt="" loading="lazy" />
        )}
        <h3>{client.business_name}</h3>
        {meta ? <p className="customer-card__meta">{meta}</p> : client.subtitle ? (
          <p className="customer-card__meta">{client.subtitle}</p>
        ) : null}
        {client.description && <p className="customer-card__desc">{client.description}</p>}
        {cta && (
          <a href={cta} target="_blank" rel="noopener noreferrer" className="customer-card__cta">
            Learn more →
          </a>
        )}
      </div>
    </AnimatedCard>
  )
}
