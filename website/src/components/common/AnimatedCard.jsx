import { motion } from 'framer-motion'
import './AnimatedCard.css'

export default function AnimatedCard({ children, className = '', delay = 0 }) {
  return (
    <motion.div
      className={`animated-card ${className}`.trim()}
      initial={{ opacity: 0, y: 28 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.2 }}
      transition={{ duration: 0.45, delay, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -8 }}
    >
      {children}
    </motion.div>
  )
}
