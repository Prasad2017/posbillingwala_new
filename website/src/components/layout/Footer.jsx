import { Link } from 'react-router-dom'
import { useSettings } from '../../context/SettingsContext'
import logoFallback from '../../assets/images/logo/pos_billingwala_logo.png'
import './Footer.css'

export default function Footer() {
  const { tagline, logoUrl } = useSettings()
  const year = new Date().getFullYear()

  return (
    <footer className="site-footer">
      <div className="site-footer__glow" aria-hidden />
      <div className="container site-footer__grid">
        <div className="site-footer__brand">
          <Link to="/">
            <img src={logoUrl || logoFallback} alt="POS Billingwala" />
          </Link>
          <p>{tagline}</p>
        </div>

        <div>
          <h4>Products</h4>
          <Link to="/software">Software</Link>
          <Link to="/products">Products</Link>
          <Link to="/pricing">Pricing</Link>
        </div>

        <div>
          <h4>Company</h4>
          <Link to="/about">About</Link>
          <Link to="/customers">Customers</Link>
          <Link to="/dealers">Dealers</Link>
          <Link to="/company">Company</Link>
        </div>

        <div>
          <h4>Support</h4>
          <Link to="/support">Support</Link>
          <Link to="/contact">Contact</Link>
          <Link to="/download-app">Download</Link>
        </div>

        <div>
          <h4>Legal</h4>
          <Link to="/privacy-policy">Privacy</Link>
          <Link to="/terms-conditions">Terms</Link>
          <Link to="/refund-policy">Refund</Link>
        </div>
      </div>

      <div className="container site-footer__bottom">
        <p>© {year} POS Billingwala. All Rights Reserved.</p>
      </div>
    </footer>
  )
}
