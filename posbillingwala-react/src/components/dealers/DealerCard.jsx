import { MapPin, Phone, MessageCircle } from 'lucide-react'
import Button from '../common/Button'
import AnimatedCard from '../common/AnimatedCard'
import { telLink, whatsappLink } from '../../utils/helpers'
import './DealerCard.css'

export default function DealerCard({ dealer, delay = 0 }) {
  const phone = dealer.mobile
  const wa = dealer.whatsapp || dealer.mobile

  return (
    <AnimatedCard className="dealer-card" delay={delay}>
      <div className="dealer-card__top">
        <h3>{dealer.dealer_name}</h3>
        {dealer.dealer_type === 'head_office' && (
          <span className="dealer-card__badge">Head Office</span>
        )}
      </div>
      {dealer.contact_person && (
        <p className="dealer-card__person">
          {dealer.contact_person}
          {dealer.role_title ? ` · ${dealer.role_title}` : ''}
        </p>
      )}
      {(dealer.area || dealer.address) && (
        <p className="dealer-card__loc">
          <MapPin size={16} />
          <span>
            {dealer.area}
            {dealer.area && dealer.address ? ' — ' : ''}
            {dealer.address}
          </span>
        </p>
      )}
      <div className="dealer-card__actions">
        {phone && (
          <Button href={telLink(phone)} variant="primary" size="small">
            <Phone size={14} /> Call
          </Button>
        )}
        {wa && (
          <Button
            href={whatsappLink(wa, `Hi, I found you on POS Billingwala dealer network.`)}
            variant="ghost"
            size="small"
            target="_blank"
            rel="noopener noreferrer"
          >
            <MessageCircle size={14} /> WhatsApp
          </Button>
        )}
        {dealer.map_url && (
          <Button href={dealer.map_url} variant="ghost" size="small" target="_blank" rel="noopener noreferrer">
            Map
          </Button>
        )}
      </div>
    </AnimatedCard>
  )
}
