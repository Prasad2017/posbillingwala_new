import { Link } from 'react-router-dom'
import './Button.css'

const VARIANTS = {
  primary: 'btn--primary',
  secondary: 'btn--secondary',
  outline: 'btn--outline',
  red: 'btn--red',
  blue: 'btn--blue',
  ghost: 'btn--ghost',
  yellow: 'btn--yellow',
}

const SIZES = {
  small: 'btn--sm',
  medium: 'btn--md',
  large: 'btn--lg',
}

export default function Button({
  children,
  variant = 'primary',
  size = 'medium',
  to,
  href,
  type = 'button',
  className = '',
  loading = false,
  disabled = false,
  fullWidth = false,
  onClick,
  ...rest
}) {
  const classes = [
    'btn',
    VARIANTS[variant] || VARIANTS.primary,
    SIZES[size] || SIZES.medium,
    fullWidth ? 'btn--full' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  const content = (
    <>
      {loading && <span className="btn__spinner" aria-hidden />}
      <span className={loading ? 'btn__label is-loading' : 'btn__label'}>{children}</span>
    </>
  )

  if (to) {
    return (
      <Link to={to} className={classes} aria-disabled={disabled || loading} {...rest}>
        {content}
      </Link>
    )
  }

  if (href) {
    return (
      <a href={href} className={classes} aria-disabled={disabled || loading} {...rest}>
        {content}
      </a>
    )
  }

  return (
    <button
      type={type}
      className={classes}
      disabled={disabled || loading}
      onClick={onClick}
      {...rest}
    >
      {content}
    </button>
  )
}
