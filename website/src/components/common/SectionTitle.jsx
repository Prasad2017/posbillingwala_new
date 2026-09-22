import { motion } from 'framer-motion'
import './SectionTitle.css'

export default function SectionTitle({
  eyebrow,
  title,
  subtitle,
  align = 'center',
  light = false,
  colorful = false,
  singleLine = false,
}) {
  return (
    <motion.div
      className={[
        'section-title',
        `section-title--${align}`,
        light ? 'section-title--light' : '',
        colorful ? 'section-title--colorful' : '',
        singleLine ? 'section-title--single' : '',
      ]
        .filter(Boolean)
        .join(' ')}
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.4 }}
      transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
    >
      {eyebrow && <span className="section-title__eyebrow">{eyebrow}</span>}
      <h2>{title}</h2>
      {subtitle && <p>{subtitle}</p>}
    </motion.div>
  )
}
