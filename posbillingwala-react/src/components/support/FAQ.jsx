import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import './FAQ.css'

const DEFAULT_FAQS = [
  {
    q: 'Is POS Billingwala suitable for restaurants and retail?',
    a: 'Yes. It is built for restaurants, retail, grocery, mess, pharmacy, garment and many other Indian businesses.',
  },
  {
    q: 'Does the app work offline?',
    a: 'Yes. You can continue billing offline and sync when connectivity returns.',
  },
  {
    q: 'Do you support thermal printers?',
    a: 'Yes. Bluetooth and USB thermal printer support is available for daily billing.',
  },
  {
    q: 'How do I get support?',
    a: 'Contact us via WhatsApp, phone or email. Your local dealer can also help with setup and training.',
  },
  {
    q: 'How do I buy or renew a plan?',
    a: 'Reach out through the Contact page or your nearest dealer. Pricing is listed on the Pricing page.',
  },
]

export default function FAQ({ items = DEFAULT_FAQS }) {
  const [open, setOpen] = useState(0)

  return (
    <div className="faq">
      {items.map((item, i) => {
        const isOpen = open === i
        return (
          <div key={item.q} className={`faq__item${isOpen ? ' is-open' : ''}`}>
            <button
              type="button"
              className="faq__q"
              aria-expanded={isOpen}
              onClick={() => setOpen(isOpen ? -1 : i)}
            >
              <span>{item.q}</span>
              <ChevronDown size={18} />
            </button>
            <AnimatePresence initial={false}>
              {isOpen && (
                <motion.div
                  className="faq__a"
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.28 }}
                >
                  <p>{item.a}</p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )
      })}
    </div>
  )
}
