import type { ReactNode } from 'react'

export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string
  subtitle?: string
  actions?: ReactNode
}) {
  return (
    <div className="row space-between" style={{ marginBottom: '1rem' }}>
      <div>
        <h1 style={{ margin: 0, fontSize: '1.35rem' }}>{title}</h1>
        {subtitle ? <p className="muted" style={{ margin: '0.25rem 0 0' }}>{subtitle}</p> : null}
      </div>
      {actions ? <div className="row">{actions}</div> : null}
    </div>
  )
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="card" style={{ textAlign: 'center', padding: '2rem' }}>
      <p className="muted" style={{ margin: 0 }}>{message}</p>
    </div>
  )
}

export function Money({ value }: { value: number }) {
  return <>₹{value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</>
}

export function LoadingBlock({ label = 'Loading…' }: { label?: string }) {
  return <div className="card muted">{label}</div>
}

export function ErrorBlock({ message }: { message: string }) {
  return <div className="card error-text">{message}</div>
}
