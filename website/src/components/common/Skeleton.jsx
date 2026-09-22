import './Skeleton.css'

export function Skeleton({ width = '100%', height = 16, className = '', style }) {
  return (
    <div
      className={`skeleton ${className}`.trim()}
      style={{ width, height, ...style }}
      aria-hidden
    />
  )
}

export function CardSkeleton() {
  return (
    <div className="card-skeleton">
      <Skeleton height={160} className="card-skeleton__media" />
      <div className="card-skeleton__body">
        <Skeleton height={18} width="70%" />
        <Skeleton height={14} width="90%" />
        <Skeleton height={14} width="55%" />
        <Skeleton height={36} width="40%" style={{ marginTop: 12, borderRadius: 999 }} />
      </div>
    </div>
  )
}
