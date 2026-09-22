import './EmptyState.css'

export default function EmptyState({ title = 'Nothing found', message = 'Try changing your search.' }) {
  return (
    <div className="empty-state">
      <h3>{title}</h3>
      <p>{message}</p>
    </div>
  )
}
