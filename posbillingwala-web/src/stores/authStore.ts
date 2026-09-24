import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { LoginPayload } from '@/api/services'
import { authApi } from '@/api/services'
import { getDeviceName, getOrCreateDeviceId } from '@/shared/device'

export type AuthPhase =
  | 'anonymous'
  | 'needsMpin'
  | 'needsStaff'
  | 'authenticated'

export interface StaffSession {
  staffId: string
  staffName: string
  mobileNumber?: string
  role?: string
  permissions: string[]
}

interface AuthState {
  phase: AuthPhase
  licenceKey: string | null
  authToken: string | null
  tokenExpiresAt: string | null
  licenceId: string | null
  shopName: string | null
  userName: string | null
  shopImage: string | null
  reportPin: string | null
  fastBilling: boolean
  dineIn: boolean
  takeAway: boolean
  mess: boolean
  userManagementEnabled: boolean
  organizationId: string | null
  branchId: string | null
  todaySaleData: string | null
  totalSaleData: string | null
  staff: StaffSession | null
  lastMessage: string | null

  applyLoginPayload: (payload: LoginPayload, opts?: { keepPhase?: AuthPhase }) => void
  setPhase: (phase: AuthPhase) => void
  setAuthToken: (token: string | null, expiresAt?: string | null) => void
  setStaff: (staff: StaffSession | null) => void
  softLogout: () => void
  hardLogout: () => void
  hasPermission: (permission?: string | null) => boolean
  refreshToken: () => Promise<string | null>
}

function flagOn(value?: string | null): boolean {
  if (value == null || value === '') return true
  const v = value.trim().toLowerCase()
  return v === '1' || v === 'true' || v === 'yes' || v === 'y'
}

function anyModuleOn(payload: LoginPayload): boolean {
  return (
    flagOn(payload.fastBilling) ||
    flagOn(payload.dineIn) ||
    flagOn(payload.takeAway) ||
    flagOn(payload.mess)
  )
}

function moduleFlags(payload: LoginPayload) {
  const all = !anyModuleOn(payload)
  return {
    fastBilling: all || flagOn(payload.fastBilling),
    dineIn: all || flagOn(payload.dineIn),
    takeAway: all || flagOn(payload.takeAway),
    mess: all || flagOn(payload.mess),
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      phase: 'anonymous',
      licenceKey: null,
      authToken: null,
      tokenExpiresAt: null,
      licenceId: null,
      shopName: null,
      userName: null,
      shopImage: null,
      reportPin: null,
      fastBilling: true,
      dineIn: true,
      takeAway: true,
      mess: true,
      userManagementEnabled: false,
      organizationId: null,
      branchId: null,
      todaySaleData: null,
      totalSaleData: null,
      staff: null,
      lastMessage: null,

      applyLoginPayload(payload, opts) {
        const modules = moduleFlags(payload)
        const userMgmt =
          String(payload.userManagementEnabled ?? '').trim() === '1'
        set({
          licenceKey: payload.licenceKey ?? get().licenceKey,
          licenceId: payload.licenceId ?? get().licenceId,
          shopName: payload.shopName ?? get().shopName,
          userName: payload.userName ?? get().userName,
          shopImage: payload.shopImage ?? get().shopImage,
          reportPin: payload.reportPin ?? get().reportPin,
          authToken: payload.authToken ?? get().authToken,
          tokenExpiresAt: payload.tokenExpiresAt ?? get().tokenExpiresAt,
          organizationId: payload.organizationId ?? get().organizationId,
          branchId: payload.branchId ?? get().branchId,
          todaySaleData: payload.todaySaleData ?? get().todaySaleData,
          totalSaleData: payload.totalSaleData ?? get().totalSaleData,
          userManagementEnabled: userMgmt,
          lastMessage: payload.message ?? null,
          ...modules,
          phase: opts?.keepPhase ?? get().phase,
        })
      },

      setPhase(phase) {
        set({ phase })
      },

      setAuthToken(token, expiresAt) {
        set({
          authToken: token,
          tokenExpiresAt: expiresAt ?? get().tokenExpiresAt,
        })
      },

      setStaff(staff) {
        set({ staff })
      },

      softLogout() {
        set({
          authToken: null,
          tokenExpiresAt: null,
          staff: null,
          phase: get().licenceKey ? 'needsMpin' : 'anonymous',
        })
      },

      hardLogout() {
        set({
          phase: 'anonymous',
          licenceKey: null,
          authToken: null,
          tokenExpiresAt: null,
          licenceId: null,
          shopName: null,
          userName: null,
          shopImage: null,
          reportPin: null,
          staff: null,
          organizationId: null,
          branchId: null,
          todaySaleData: null,
          totalSaleData: null,
          userManagementEnabled: false,
          lastMessage: null,
        })
      },

      hasPermission(permission) {
        if (!permission) return true
        const { staff, userManagementEnabled } = get()
        if (!userManagementEnabled || !staff) return true
        if (staff.permissions.includes('*')) return true
        return staff.permissions.includes(permission)
      },

      async refreshToken() {
        const { licenceKey } = get()
        if (!licenceKey) return null
        const deviceId = getOrCreateDeviceId()
        const result = await authApi.refreshAuthToken(licenceKey, deviceId)
        if (!result) return null
        set({
          authToken: result.token,
          tokenExpiresAt: result.expiresAt,
        })
        return result.token
      },
    }),
    {
      name: 'pb-auth-session',
      partialize: (state) => ({
        phase: state.phase === 'authenticated' ? 'needsMpin' : state.phase,
        licenceKey: state.licenceKey,
        licenceId: state.licenceId,
        shopName: state.shopName,
        userName: state.userName,
        shopImage: state.shopImage,
        reportPin: state.reportPin,
        fastBilling: state.fastBilling,
        dineIn: state.dineIn,
        takeAway: state.takeAway,
        mess: state.mess,
        userManagementEnabled: state.userManagementEnabled,
        organizationId: state.organizationId,
        branchId: state.branchId,
        todaySaleData: state.todaySaleData,
        totalSaleData: state.totalSaleData,
        // Never persist bearer token across full page reloads for security —
        // require MPIN unlock (soft session). Staff must re-login after refresh.
        authToken: null,
        tokenExpiresAt: null,
        staff: null,
      }),
    },
  ),
)

export function useUserId(): string | null {
  return useAuthStore((s) => s.licenceId)
}

export { getDeviceName, getOrCreateDeviceId }
