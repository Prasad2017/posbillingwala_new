import './Loader.css'

export default function Loader({ label = 'Loading…' }) {
  return (
    <div className="loader" role="status" aria-live="polite">
      <span className="loader__ring" />
      <span className="loader__text">{label}</span>
    </div>
  )
}
