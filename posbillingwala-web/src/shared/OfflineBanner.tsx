import { useEffect } from 'react'
import { useOnlineStore } from '@/stores/onlineStore'
import { useI18n } from '@/i18n'

export function OfflineBanner() {
  const online = useOnlineStore((s) => s.online)
  const t = useI18n((s) => s.t)

  if (online) return null

  return (
    <div className="offline-banner">
      <span>{t('onlineRequired')}</span>
      <button
        type="button"
        className="btn btn-secondary"
        onClick={() => window.location.reload()}
      >
        {t('reconnect')}
      </button>
    </div>
  )
}

export function useBlockWhenOffline() {
  const online = useOnlineStore((s) => s.online)
  useEffect(() => {
    // no-op hook for callers that need reactivity
  }, [online])
  return !online
}
