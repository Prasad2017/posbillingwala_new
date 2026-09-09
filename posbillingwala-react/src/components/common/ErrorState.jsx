import { AlertTriangle } from 'lucide-react'
import Button from './Button'
import './ErrorState.css'

export default function ErrorState({
  title = 'Unable To Load Data',
  message = 'Please try again.',
  onRetry,
}) {
  return (
    <div className="error-state" role="alert">
      <div className="error-state__icon">
        <AlertTriangle size={28} />
      </div>
      <h3>{title}</h3>
      <p>{message}</p>
      {onRetry && (
        <Button variant="primary" size="small" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  )
}
