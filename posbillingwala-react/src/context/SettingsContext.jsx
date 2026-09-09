import { createContext, useContext, useMemo } from 'react'
import { useApi } from '../hooks/useApi'
import { getWebsiteSettings } from '../services/websiteService'
import {
  DEFAULT_COMPANY,
  DEFAULT_EMAIL,
  DEFAULT_PHONE,
  DEFAULT_PLAY_STORE,
  DEFAULT_WHATSAPP,
  SITE_TAGLINE,
} from '../utils/constants'

const SettingsContext = createContext(null)

export function SettingsProvider({ children }) {
  const { data, loading, error, refetch } = useApi(getWebsiteSettings, [])

  const value = useMemo(() => {
    const s = data || {}
    return {
      settings: s,
      loading,
      error,
      refetch,
      companyName: s.legal_company_name || DEFAULT_COMPANY,
      tagline: s.brand_tagline || SITE_TAGLINE,
      phone: s.support_phone || DEFAULT_PHONE,
      whatsapp: s.support_whatsapp || s.support_phone || DEFAULT_WHATSAPP,
      email: s.support_email || DEFAULT_EMAIL,
      address: s.office_address || '',
      gstin: s.gstin || '',
      hours: s.business_hours || '',
      playStoreUrl: s.play_store_url || DEFAULT_PLAY_STORE,
      appVersion: s.app_latest_version || '',
      logoUrl: s.logo_url || '',
      faviconUrl: s.favicon_url || '',
    }
  }, [data, loading, error, refetch])

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>
}

export function useSettings() {
  const ctx = useContext(SettingsContext)
  if (!ctx) throw new Error('useSettings must be used within SettingsProvider')
  return ctx
}
