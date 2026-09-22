import {
  ArrowRight,
  Package,
  MonitorSmartphone,
  Printer,
  ScrollText,
  Cable,
} from 'lucide-react'
import Button from '../common/Button'
import AnimatedCard from '../common/AnimatedCard'
import IconBadge from '../common/IconBadge'
import './ProductCard.css'

const CATEGORY_LABELS = {
  software: 'Software',
  hardware: 'Hardware',
  consumables: 'Consumables',
  accessories: 'Accessories',
}

const CATEGORY_META = {
  software: { icon: MonitorSmartphone, tone: 'blue' },
  hardware: { icon: Printer, tone: 'orange' },
  consumables: { icon: ScrollText, tone: 'green' },
  accessories: { icon: Cable, tone: 'purple' },
}

export default function ProductCard({ product, delay = 0, onEnquire }) {
  const meta = CATEGORY_META[product.category] || { icon: Package, tone: 'cyan' }

  return (
    <AnimatedCard className={`product-card product-card--${meta.tone}`} delay={delay}>
      <div className={`product-card__media product-card__media--${meta.tone}`}>
        <IconBadge icon={meta.icon} tone={meta.tone} size="lg" />
        {product.category && (
          <span className="product-card__cat">
            {CATEGORY_LABELS[product.category] || product.category}
          </span>
        )}
      </div>
      <div className="product-card__body">
        <h3>{product.name}</h3>
        <p>{product.description || 'Reliable product for your billing setup.'}</p>
        <div className="product-card__actions">
          <Button to="/contact" variant="primary" size="small">
            View Details <ArrowRight size={14} />
          </Button>
          <Button
            variant="ghost"
            size="small"
            onClick={() => onEnquire?.(product)}
            to={
              onEnquire
                ? undefined
                : `/contact?subject=${encodeURIComponent(`Enquiry: ${product.name}`)}`
            }
          >
            Enquire
          </Button>
        </div>
      </div>
    </AnimatedCard>
  )
}
