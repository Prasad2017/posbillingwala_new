import './IconBadge.css'

const TONES = {
  blue: 'icon-badge--blue',
  red: 'icon-badge--red',
  green: 'icon-badge--green',
  orange: 'icon-badge--orange',
  purple: 'icon-badge--purple',
  cyan: 'icon-badge--cyan',
  pink: 'icon-badge--pink',
  amber: 'icon-badge--amber',
  teal: 'icon-badge--teal',
  indigo: 'icon-badge--indigo',
}

export default function IconBadge({
  icon: Icon,
  tone = 'blue',
  size = 'md',
  className = '',
}) {
  if (!Icon) return null
  const px = size === 'lg' ? 26 : size === 'sm' ? 16 : 22
  return (
    <span className={`icon-badge icon-badge--${size} ${TONES[tone] || TONES.blue} ${className}`.trim()}>
      <Icon size={px} strokeWidth={2.1} />
    </span>
  )
}
