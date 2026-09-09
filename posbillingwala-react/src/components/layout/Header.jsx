import { useEffect, useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { Menu, Phone, X } from 'lucide-react'
import { useScrollPosition } from '../../hooks/useScrollPosition'
import { useSettings } from '../../context/SettingsContext'
import { NAV_LINKS } from '../../utils/constants'
import { formatPhoneDisplay, telLink } from '../../utils/helpers'
import logoFallback from '../../assets/images/logo/pos_billingwala_logo.png'
import Button from '../common/Button'
import './Header.css'

export default function Header() {
  const scrolled = useScrollPosition(16)
  const { phone, logoUrl, gstin } = useSettings()
  const [open, setOpen] = useState(false)

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [open])

  const close = () => setOpen(false)

  return (
    <>
      <header className={`site-header${scrolled || open ? ' is-scrolled' : ''}`}>
        <div className="site-header__glow" aria-hidden />
        <div className="container site-header__inner">
          <Link to="/" className="site-header__brand" onClick={close}>
            <img src={logoUrl || logoFallback} alt="POS Billingwala" />
            {gstin && (
              <span className="site-header__gstin" title="GSTIN">
                <small>GSTIN</small>
                <strong>{gstin}</strong>
              </span>
            )}
          </Link>

          <nav className="site-header__nav hide-mobile" aria-label="Primary">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  `site-header__link${isActive ? ' is-active' : ''}`
                }
              >
                <span>{link.label}</span>
              </NavLink>
            ))}
          </nav>

          <div className="site-header__actions">
            <a className="site-header__phone" href={telLink(phone)}>
              <span className="site-header__phone-icon">
                <Phone size={14} strokeWidth={2.4} />
              </span>
              <span className="site-header__phone-text hide-mobile">{formatPhoneDisplay(phone)}</span>
            </a>
            <button
              type="button"
              className={`site-header__menu-btn hide-desktop${open ? ' is-open' : ''}`}
              aria-label={open ? 'Close menu' : 'Open menu'}
              aria-expanded={open}
              onClick={() => setOpen((v) => !v)}
            >
              {open ? <X size={22} /> : <Menu size={22} />}
            </button>
          </div>
        </div>
      </header>

      <div className={`mobile-drawer${open ? ' is-open' : ''}`} aria-hidden={!open}>
        <div className="mobile-drawer__panel">
          <div className="mobile-drawer__head">
            <p>Menu</p>
            <span>POS Billingwala</span>
          </div>
          <nav className="mobile-drawer__nav" aria-label="Mobile">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  `mobile-drawer__link${isActive ? ' is-active' : ''}`
                }
                onClick={close}
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="mobile-drawer__cta">
            <Button to="/contact" variant="ghost" fullWidth onClick={close}>
              Book Free Demo
            </Button>
            <a className="mobile-drawer__phone" href={telLink(phone)} onClick={close}>
              <span className="site-header__phone-icon">
                <Phone size={14} />
              </span>
              {formatPhoneDisplay(phone)}
            </a>
          </div>
        </div>
      </div>
    </>
  )
}
