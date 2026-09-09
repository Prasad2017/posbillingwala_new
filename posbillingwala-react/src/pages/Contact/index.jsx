import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Building2, Phone, Mail, MapPin, Clock, BadgeCheck } from 'lucide-react'
import Seo from '../../components/common/Seo'
import Button from '../../components/common/Button'
import IconBadge from '../../components/common/IconBadge'
import { submitContact } from '../../services/contactService'
import { useSettings } from '../../context/SettingsContext'
import { formatPhoneDisplay, telLink, mapsEmbedUrl } from '../../utils/helpers'
import contactVisual from '../../assets/images/business/services-image-03.jpg'
import contactAccent from '../../assets/images/business/services-image-02.jpg'
import '../pages.css'
import './Contact.css'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const INITIAL = {
  name: '',
  email: '',
  subject: '',
  message: '',
}

export default function Contact() {
  const [searchParams] = useSearchParams()
  const { companyName, phone, email, address, gstin, hours } = useSettings()
  const [form, setForm] = useState(INITIAL)
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState({ type: '', text: '' })

  useEffect(() => {
    const subject = searchParams.get('subject')
    if (subject) {
      setForm((prev) => ({ ...prev, subject }))
    }
  }, [searchParams])

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
    setErrors((prev) => ({ ...prev, [key]: '' }))
    setFeedback({ type: '', text: '' })
  }

  function validate() {
    const next = {}
    if (!form.name.trim()) next.name = 'Name is required.'
    if (!form.email.trim()) next.email = 'Email is required.'
    else if (!EMAIL_RE.test(form.email.trim())) next.email = 'Enter a valid email address.'
    if (!form.message.trim()) next.message = 'Message is required.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFeedback({ type: '', text: '' })
    if (!validate()) return

    setSubmitting(true)
    try {
      await submitContact({
        name: form.name.trim(),
        email: form.email.trim(),
        subject: form.subject.trim(),
        message: form.message.trim(),
      })
      setFeedback({
        type: 'success',
        text: 'Thank you! Your message has been sent. We’ll get back to you soon.',
      })
      setForm({
        ...INITIAL,
        subject: searchParams.get('subject') || '',
      })
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err?.message || 'Unable to send message. Please try again.',
      })
    } finally {
      setSubmitting(false)
    }
  }

  const mapSrc = mapsEmbedUrl(address)

  const infoCards = [
    companyName && {
      key: 'company',
      icon: Building2,
      tone: 'blue',
      label: 'Company',
      value: companyName,
    },
    gstin && {
      key: 'gstin',
      icon: BadgeCheck,
      tone: 'purple',
      label: 'GSTIN',
      value: gstin,
    },
    phone && {
      key: 'phone',
      icon: Phone,
      tone: 'green',
      label: 'Phone',
      value: formatPhoneDisplay(phone),
      href: telLink(phone),
    },
    email && {
      key: 'email',
      icon: Mail,
      tone: 'orange',
      label: 'Email',
      value: email,
      href: `mailto:${email}`,
    },
    address && {
      key: 'address',
      icon: MapPin,
      tone: 'pink',
      label: 'Address',
      value: address,
    },
    hours && {
      key: 'hours',
      icon: Clock,
      tone: 'cyan',
      label: 'Working Hours',
      value: hours,
    },
  ].filter(Boolean)

  return (
    <>
      <Seo
        title="Get in Touch"
        description="Contact POS Billingwala about software, products, demos, dealers or support."
        path="/contact"
      />

      <section className="page-hero">
        <div className="container">
          <span className="page-kicker">CONTACT</span>
          <h1>Get in Touch. We&apos;d Love to Help.</h1>
          <p className="page-hero-lead">
            Talk to us about software, products, demos, dealers or support.
          </p>
        </div>
      </section>

      <section className="section page-section--soft">
        <div className="container">
          <div className="contact-intro">
            <div>
              <span className="section-title__eyebrow section-title__eyebrow--badge">Let&apos;s talk</span>
              <h2 className="contact-intro__title page-heading">Reach our team anytime</h2>
              <p className="contact-intro__lead">
                Reach out and our team will help you with demos, setup, dealers or support.
              </p>
            </div>
            <div className="contact-visual">
              <img className="contact-visual__main" src={contactVisual} alt="Contact POS Billingwala" loading="lazy" />
              <img className="contact-visual__float" src={contactAccent} alt="" loading="lazy" />
            </div>
          </div>

          {infoCards.length > 0 && (
            <div className="contact-info-grid">
              {infoCards.map((card) => {
                const content = (
                  <>
                    <div className="contact-info-card__icon">
                      <IconBadge icon={card.icon} tone={card.tone} />
                    </div>
                    <strong>{card.label}</strong>
                    <span>{card.value}</span>
                  </>
                )
                return card.href ? (
                  <a
                    key={card.key}
                    className={`contact-info-card contact-info-card--${card.tone}`}
                    href={card.href}
                  >
                    {content}
                  </a>
                ) : (
                  <article
                    key={card.key}
                    className={`contact-info-card contact-info-card--${card.tone}`}
                  >
                    {content}
                  </article>
                )
              })}
            </div>
          )}
        </div>
      </section>

      <section className="section page-section--lavender">
        <div className="container contact-grid">
          <form className="contact-form" onSubmit={handleSubmit} noValidate>
            <h2 className="contact-form__title page-heading">Send us a message</h2>

            <div className="contact-form__row">
              <label>
                Name
                <input
                  type="text"
                  name="name"
                  autoComplete="name"
                  placeholder="Your Name"
                  value={form.name}
                  onChange={(e) => updateField('name', e.target.value)}
                  aria-invalid={Boolean(errors.name)}
                  required
                />
                {errors.name && <span className="field-error">{errors.name}</span>}
              </label>
              <label>
                Phone / Email
                <input
                  type="email"
                  name="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={(e) => updateField('email', e.target.value)}
                  aria-invalid={Boolean(errors.email)}
                  required
                />
                {errors.email && <span className="field-error">{errors.email}</span>}
              </label>
            </div>

            <label>
              Subject
              <input
                type="text"
                name="subject"
                placeholder="How can we help?"
                value={form.subject}
                onChange={(e) => updateField('subject', e.target.value)}
              />
            </label>

            <label>
              Message
              <textarea
                name="message"
                rows={5}
                placeholder="Your message"
                value={form.message}
                onChange={(e) => updateField('message', e.target.value)}
                aria-invalid={Boolean(errors.message)}
                required
              />
              {errors.message && <span className="field-error">{errors.message}</span>}
            </label>

            <Button type="submit" variant="primary" size="large" loading={submitting} disabled={submitting}>
              Send Message
            </Button>

            {feedback.text && (
              <div
                className={`contact-feedback contact-feedback--${feedback.type}`}
                role="status"
                aria-live="polite"
              >
                {feedback.text}
              </div>
            )}
          </form>

          <aside className="contact-side">
            <div className="contact-side__card">
              <h3>Need faster help?</h3>
              <p>Call us or message on WhatsApp for quick support and demos.</p>
              <div className="contact-side__actions">
                {phone && (
                  <Button href={telLink(phone)} variant="primary">
                    Call Now
                  </Button>
                )}
                <Button to="/support" variant="ghost">
                  Visit Support
                </Button>
              </div>
            </div>

            {mapSrc && (
              <div className="contact-map-wrap contact-map-wrap--side">
                <iframe
                  className="contact-map"
                  title="POS Billingwala office location"
                  src={mapSrc}
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              </div>
            )}
          </aside>
        </div>
      </section>
    </>
  )
}
