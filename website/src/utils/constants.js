export const SITE_NAME = 'POS Billingwala'
export const SITE_TAGLINE = 'Smart Billing. Trusted Support. Better Business.'
export const DEFAULT_PLAY_STORE =
  'https://play.google.com/store/apps/details?id=com.pos_billingwala'
export const DEFAULT_WHATSAPP = '8983149299'
export const DEFAULT_PHONE = '8983149299'
export const DEFAULT_EMAIL = 'support@posbillingwala.com'
export const DEFAULT_COMPANY = 'CANA Tech Solutions Private Limited'

export const NAV_LINKS = [
  { to: '/', label: 'Home', end: true },
  { to: '/products', label: 'Products' },
  { to: '/software', label: 'Software' },
  { to: '/pricing', label: 'Pricing' },
  { to: '/dealers', label: 'Dealers' },
  { to: '/customers', label: 'Customers' },
  { to: '/support', label: 'Support' },
]

export const BUSINESS_TYPES = [
  { id: 'restaurant', label: 'Restaurant', icon: 'UtensilsCrossed' },
  { id: 'grocery', label: 'Grocery', icon: 'ShoppingCart' },
  { id: 'retail', label: 'Retail', icon: 'Store' },
  { id: 'pharmacy', label: 'Pharmacy', icon: 'Pill' },
  { id: 'cafe', label: 'Cafe', icon: 'Coffee' },
  { id: 'bakery', label: 'Bakery', icon: 'Cake' },
  { id: 'garment', label: 'Garment', icon: 'Shirt' },
  { id: 'hotel', label: 'Hotel', icon: 'Building2' },
  { id: 'wholesale', label: 'Wholesale', icon: 'Boxes' },
  { id: 'mess', label: 'Mess / Canteen', icon: 'Soup' },
]

export const PRODUCT_CATEGORIES = [
  { id: '', label: 'All' },
  { id: 'software', label: 'Software' },
  { id: 'hardware', label: 'Hardware' },
  { id: 'consumables', label: 'Consumables' },
  { id: 'accessories', label: 'Accessories' },
]

export const CMS_SLUGS = {
  about: 'about',
  company: 'company',
  support: 'support',
  privacy: 'privacy',
  terms: 'terms',
  refund: 'refund-renewal',
}

export const LEGACY_REDIRECTS = {
  '/index.html': '/',
  '/products.html': '/products',
  '/software.html': '/software',
  '/pricing.html': '/pricing',
  '/dealers.html': '/dealers',
  '/customers.html': '/customers',
  '/support.html': '/support',
  '/download.html': '/download-app',
  '/contact.html': '/contact',
  '/company.html': '/company',
  '/about.html': '/about',
  '/privacy.html': '/privacy-policy',
  '/terms.html': '/terms-conditions',
  '/refund.html': '/refund-policy',
}
